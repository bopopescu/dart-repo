// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/globals.h"  // Needed here to get TARGET_ARCH_X64.
#if defined(TARGET_ARCH_X64)

#include "vm/flow_graph_compiler.h"

#include "lib/error.h"
#include "vm/ast_printer.h"
#include "vm/il_printer.h"
#include "vm/locations.h"
#include "vm/object_store.h"
#include "vm/parser.h"
#include "vm/stub_code.h"

namespace dart {

DECLARE_FLAG(bool, enable_type_checks);
DECLARE_FLAG(bool, print_ast);
DECLARE_FLAG(bool, print_scopes);
DECLARE_FLAG(bool, trace_functions);


void DeoptimizationStub::GenerateCode(FlowGraphCompiler* compiler) {
  Assembler* assem = compiler->assembler();
#define __ assem->
  __ Comment("Deopt stub for id %d", deopt_id_);
  __ Bind(entry_label());
  for (intptr_t i = 0; i < registers_.length(); i++) {
    if (registers_[i] != kNoRegister) {
      __ pushq(registers_[i]);
    }
  }
  if (compiler->IsLeaf()) {
    Label L;
    __ call(&L);
    const intptr_t offset = assem->CodeSize();
    __ Bind(&L);
    __ popq(RAX);
    __ subq(RAX,
        Immediate(offset - AssemblerMacros::kOffsetOfSavedPCfromEntrypoint));
    __ movq(Address(RBP, -kWordSize), RAX);
  }
  __ movq(RAX, Immediate(Smi::RawValue(reason_)));
  __ call(&StubCode::DeoptimizeLabel());
  compiler->AddCurrentDescriptor(PcDescriptors::kOther,
                                 deopt_id_,
                                 deopt_token_pos_,
                                 try_index_);
#undef __
}


#define __ assembler()->


// Fall through if bool_register contains null.
void FlowGraphCompiler::GenerateBoolToJump(Register bool_register,
                                           Label* is_true,
                                           Label* is_false) {
  const Immediate raw_null =
      Immediate(reinterpret_cast<intptr_t>(Object::null()));
  Label fall_through;
  __ cmpq(bool_register, raw_null);
  __ j(EQUAL, &fall_through, Assembler::kNearJump);
  __ CompareObject(bool_register, bool_true());
  __ j(EQUAL, is_true);
  __ jmp(is_false);
  __ Bind(&fall_through);
}


// Clobbers RCX.
RawSubtypeTestCache* FlowGraphCompiler::GenerateCallSubtypeTestStub(
    TypeTestStubKind test_kind,
    Register instance_reg,
    Register type_arguments_reg,
    Register temp_reg,
    Label* is_instance_lbl,
    Label* is_not_instance_lbl) {
  const SubtypeTestCache& type_test_cache =
      SubtypeTestCache::ZoneHandle(SubtypeTestCache::New());
  const Immediate raw_null =
      Immediate(reinterpret_cast<intptr_t>(Object::null()));
  __ LoadObject(temp_reg, type_test_cache);
  __ pushq(temp_reg);  // Subtype test cache.
  __ pushq(instance_reg);  // Instance.
  if (test_kind == kTestTypeOneArg) {
    ASSERT(type_arguments_reg == kNoRegister);
    __ pushq(raw_null);
    __ call(&StubCode::Subtype1TestCacheLabel());
  } else if (test_kind == kTestTypeTwoArgs) {
    ASSERT(type_arguments_reg == kNoRegister);
    __ pushq(raw_null);
    __ call(&StubCode::Subtype2TestCacheLabel());
  } else if (test_kind == kTestTypeThreeArgs) {
    __ pushq(type_arguments_reg);
    __ call(&StubCode::Subtype3TestCacheLabel());
  } else {
    UNREACHABLE();
  }
  // Result is in RCX: null -> not found, otherwise Bool::True or Bool::False.
  ASSERT(instance_reg != RCX);
  ASSERT(temp_reg != RCX);
  __ popq(instance_reg);  // Discard.
  __ popq(instance_reg);  // Restore receiver.
  __ popq(temp_reg);  // Discard.
  GenerateBoolToJump(RCX, is_instance_lbl, is_not_instance_lbl);
  return type_test_cache.raw();
}


// Jumps to labels 'is_instance' or 'is_not_instance' respectively, if
// type test is conclusive, otherwise fallthrough if a type test could not
// be completed.
// RAX: instance (must survive).
// Clobbers R10.
RawSubtypeTestCache*
FlowGraphCompiler::GenerateInstantiatedTypeWithArgumentsTest(
    intptr_t cid,
    intptr_t token_pos,
    const AbstractType& type,
    Label* is_instance_lbl,
    Label* is_not_instance_lbl) {
  ASSERT(type.IsInstantiated());
  const Class& type_class = Class::ZoneHandle(type.type_class());
  ASSERT(type_class.HasTypeArguments());
  const Register kInstanceReg = RAX;
  // A Smi object cannot be the instance of a parameterized class.
  __ testq(kInstanceReg, Immediate(kSmiTagMask));
  __ j(ZERO, is_not_instance_lbl);
  const AbstractTypeArguments& type_arguments =
      AbstractTypeArguments::ZoneHandle(type.arguments());
  const bool is_raw_type = type_arguments.IsNull() ||
      type_arguments.IsRaw(type_arguments.Length());
  if (is_raw_type) {
    const Register kClassIdReg = R10;
    // Dynamic type argument, check only classes.
    // List is a very common case.
    __ LoadClassId(kClassIdReg, kInstanceReg);
    if (!type_class.is_interface()) {
      __ cmpl(kClassIdReg, Immediate(type_class.id()));
      __ j(EQUAL, is_instance_lbl);
    }
    if (type.IsListInterface()) {
      GenerateListTypeCheck(kClassIdReg, is_instance_lbl);
    }
    return GenerateSubtype1TestCacheLookup(
        cid, token_pos, type_class, is_instance_lbl, is_not_instance_lbl);
  }
  // If one type argument only, check if type argument is Object or Dynamic.
  if (type_arguments.Length() == 1) {
    const AbstractType& tp_argument = AbstractType::ZoneHandle(
        type_arguments.TypeAt(0));
    ASSERT(!tp_argument.IsMalformed());
    if (tp_argument.IsType()) {
      ASSERT(tp_argument.HasResolvedTypeClass());
      // Check if type argument is dynamic or Object.
      const Type& object_type = Type::Handle(Type::ObjectType());
      Error& malformed_error = Error::Handle();
      if (object_type.IsSubtypeOf(tp_argument, &malformed_error)) {
        // Instance class test only necessary.
        return GenerateSubtype1TestCacheLookup(
            cid, token_pos, type_class, is_instance_lbl, is_not_instance_lbl);
      }
    }
  }
  // Regular subtype test cache involving instance's type arguments.
  const Register kTypeArgumentsReg = kNoRegister;
  const Register kTempReg = R10;
  return GenerateCallSubtypeTestStub(kTestTypeTwoArgs,
                                     kInstanceReg,
                                     kTypeArgumentsReg,
                                     kTempReg,
                                     is_instance_lbl,
                                     is_not_instance_lbl);
}


void FlowGraphCompiler::CheckClassIds(Register class_id_reg,
                                      const GrowableArray<intptr_t>& class_ids,
                                      Label* is_equal_lbl,
                                      Label* is_not_equal_lbl) {
  for (intptr_t i = 0; i < class_ids.length(); i++) {
    __ cmpl(class_id_reg, Immediate(class_ids[i]));
    __ j(EQUAL, is_equal_lbl);
  }
  __ jmp(is_not_equal_lbl);
}


// Testing against an instantiated type with no arguments, without
// SubtypeTestCache.
// RAX: instance to test against (preserved).
// Clobbers R10, R13.
void FlowGraphCompiler::GenerateInstantiatedTypeNoArgumentsTest(
    intptr_t cid,
    intptr_t token_pos,
    const AbstractType& type,
    Label* is_instance_lbl,
    Label* is_not_instance_lbl) {
  ASSERT(type.IsInstantiated());
  const Class& type_class = Class::Handle(type.type_class());
  ASSERT(!type_class.HasTypeArguments());

  const Register kInstanceReg = RAX;
  Label compare_classes;
  __ testq(kInstanceReg, Immediate(kSmiTagMask));
  __ j(NOT_ZERO, &compare_classes, Assembler::kNearJump);
  // Instance is Smi, check directly.
  const Class& smi_class = Class::Handle(Smi::Class());
  // TODO(regis): We should introduce a SmiType.
  Error& malformed_error = Error::Handle();
  if (smi_class.IsSubtypeOf(TypeArguments::Handle(),
                            type_class,
                            TypeArguments::Handle(),
                            &malformed_error)) {
    __ jmp(is_instance_lbl);
  } else {
    __ jmp(is_not_instance_lbl);
  }
  // Compare if the classes are equal.
  __ Bind(&compare_classes);
  const Register kClassIdReg = R10;
  __ LoadClassId(kClassIdReg, kInstanceReg);
  // If type is an interface, we can skip the class equality check.
  if (!type_class.is_interface()) {
    __ cmpl(kClassIdReg, Immediate(type_class.id()));
    __ j(EQUAL, is_instance_lbl);
  }
  // Bool interface can be implemented only by core class Bool.
  // (see ClassFinalizer::ResolveInterfaces for list of restricted interfaces).
  if (type.IsBoolInterface()) {
    __ cmpl(kClassIdReg, Immediate(kBool));
    __ j(EQUAL, is_instance_lbl);
    __ jmp(is_not_instance_lbl);
    return;
  }
  if (type.IsFunctionInterface()) {
    // Check if instance is a closure.
    const Immediate raw_null =
        Immediate(reinterpret_cast<intptr_t>(Object::null()));
    __ LoadClassById(R13, kClassIdReg);
    __ movq(R13, FieldAddress(R13, Class::signature_function_offset()));
    __ cmpq(R13, raw_null);
    __ j(NOT_EQUAL, is_instance_lbl);
    __ jmp(is_not_instance_lbl);
    return;
  }
  // Custom checking for numbers (Smi, Mint, Bigint and Double).
  // Note that instance is not Smi(checked above).
  if (type.IsSubtypeOf(
          Type::Handle(Type::NumberInterface()), &malformed_error)) {
    GenerateNumberTypeCheck(
        kClassIdReg, type, is_instance_lbl, is_not_instance_lbl);
    return;
  }
  if (type.IsStringInterface()) {
    GenerateStringTypeCheck(kClassIdReg, is_instance_lbl, is_not_instance_lbl);
    return;
  }
  // Otherwise fallthrough.
}


// Uses SubtypeTestCache to store instance class and result.
// RAX: instance to test.
// Clobbers R10, R13.
// Immediate class test already done.
// TODO(srdjan): Implement a quicker subtype check, as type test
// arrays can grow too high, but they may be useful when optimizing
// code (type-feedback).
RawSubtypeTestCache* FlowGraphCompiler::GenerateSubtype1TestCacheLookup(
    intptr_t cid,
    intptr_t token_pos,
    const Class& type_class,
    Label* is_instance_lbl,
    Label* is_not_instance_lbl) {
  const Register kInstanceReg = RAX;
  __ LoadClass(R10, kInstanceReg);
  // R10: instance class.
  // Check immediate superclass equality.
  __ movq(R13, FieldAddress(R10, Class::super_type_offset()));
  __ movq(R13, FieldAddress(R13, Type::type_class_offset()));
  __ CompareObject(R13, type_class);
  __ j(EQUAL, is_instance_lbl);

  const Register kTypeArgumentsReg = kNoRegister;
  const Register kTempReg = R10;
  return GenerateCallSubtypeTestStub(kTestTypeOneArg,
                                     kInstanceReg,
                                     kTypeArgumentsReg,
                                     kTempReg,
                                     is_instance_lbl,
                                     is_not_instance_lbl);
}


// Generates inlined check if 'type' is a type parameter or type itsef
// RAX: instance (preserved).
// Clobbers RDI, RDX, R10.
RawSubtypeTestCache* FlowGraphCompiler::GenerateUninstantiatedTypeTest(
    intptr_t cid,
    intptr_t token_pos,
    const AbstractType& type,
    Label* is_instance_lbl,
    Label* is_not_instance_lbl) {
  ASSERT(!type.IsInstantiated());
  // Skip check if destination is a dynamic type.
  const Immediate raw_null =
      Immediate(reinterpret_cast<intptr_t>(Object::null()));
  if (type.IsTypeParameter()) {
    const TypeParameter& type_param = TypeParameter::Cast(type);
    // Load instantiator (or null) and instantiator type arguments on stack.
    __ movq(RDX, Address(RSP, 0));  // Get instantiator type arguments.
    // RDX: instantiator type arguments.
    // Check if type argument is Dynamic.
    __ cmpq(RDX, raw_null);
    __ j(EQUAL, is_instance_lbl);
    // Can handle only type arguments that are instances of TypeArguments.
    // (runtime checks canonicalize type arguments).
    Label fall_through;
    __ CompareClassId(RDX, kTypeArguments);
    __ j(NOT_EQUAL, &fall_through);
    __ movq(RDI,
        FieldAddress(RDX, TypeArguments::type_at_offset(type_param.index())));
    // RDI: Concrete type of type.
    // Check if type argument is dynamic.
    __ CompareObject(RDI, Type::ZoneHandle(Type::DynamicType()));
    __ j(EQUAL,  is_instance_lbl);
    __ cmpq(RDI, raw_null);
    __ j(EQUAL,  is_instance_lbl);
    const Type& object_type = Type::ZoneHandle(Type::ObjectType());
    __ CompareObject(RDI, object_type);
    __ j(EQUAL,  is_instance_lbl);

    // For Smi check quickly against int and num interfaces.
    Label not_smi;
    __ testq(RAX, Immediate(kSmiTagMask));  // Value is Smi?
    __ j(NOT_ZERO, &not_smi, Assembler::kNearJump);
    __ CompareObject(RDI, Type::ZoneHandle(Type::IntInterface()));
    __ j(EQUAL,  is_instance_lbl);
    __ CompareObject(RDI, Type::ZoneHandle(Type::NumberInterface()));
    __ j(EQUAL,  is_instance_lbl);
    // Smi must be handled in runtime.
    __ jmp(&fall_through);

    __ Bind(&not_smi);
    // RDX: instantiator type arguments.
    // RAX: instance.
    const Register kInstanceReg = RAX;
    const Register kTypeArgumentsReg = RDX;
    const Register kTempReg = R10;
    const SubtypeTestCache& type_test_cache =
        SubtypeTestCache::ZoneHandle(
            GenerateCallSubtypeTestStub(kTestTypeThreeArgs,
                                        kInstanceReg,
                                        kTypeArgumentsReg,
                                        kTempReg,
                                        is_instance_lbl,
                                        is_not_instance_lbl));
    __ Bind(&fall_through);
    return type_test_cache.raw();
  }
  if (type.IsType()) {
    const Register kInstanceReg = RAX;
    const Register kTypeArgumentsReg = RDX;
    __ testq(kInstanceReg, Immediate(kSmiTagMask));  // Is instance Smi?
    __ j(ZERO, is_not_instance_lbl);
    __ movq(kTypeArgumentsReg, Address(RSP, 0));  // Instantiator type args.
    // Uninstantiated type class is known at compile time, but the type
    // arguments are determined at runtime by the instantiator.
    const Register kTempReg = R10;
    return GenerateCallSubtypeTestStub(kTestTypeThreeArgs,
                                       kInstanceReg,
                                       kTypeArgumentsReg,
                                       kTempReg,
                                       is_instance_lbl,
                                       is_not_instance_lbl);
  }
  return SubtypeTestCache::null();
}


// Inputs:
// - RAX: instance to test against (preserved).
// - RDX: optional instantiator type arguments (preserved).
// Clobbers R10, R13.
// Returns:
// - preserved instance in RAX and optional instantiator type arguments in RDX.
// Note that this inlined code must be followed by the runtime_call code, as it
// may fall through to it. Otherwise, this inline code will jump to the label
// is_instance or to the label is_not_instance.
RawSubtypeTestCache* FlowGraphCompiler::GenerateInlineInstanceof(
    intptr_t cid,
    intptr_t token_pos,
    const AbstractType& type,
    Label* is_instance_lbl,
    Label* is_not_instance_lbl) {
  if (type.IsInstantiated()) {
    const Class& type_class = Class::ZoneHandle(type.type_class());
    // A Smi object cannot be the instance of a parameterized class.
    // A class equality check is only applicable with a dst type of a
    // non-parameterized class or with a raw dst type of a parameterized class.
    if (type_class.HasTypeArguments()) {
      return GenerateInstantiatedTypeWithArgumentsTest(cid,
                                                       token_pos,
                                                       type,
                                                       is_instance_lbl,
                                                       is_not_instance_lbl);
      // Fall through to runtime call.
    } else {
      GenerateInstantiatedTypeNoArgumentsTest(cid,
                                              token_pos,
                                              type,
                                              is_instance_lbl,
                                              is_not_instance_lbl);
      // If test non-conclusive so far, try the inlined type-test cache.
      // 'type' is known at compile time.
      return GenerateSubtype1TestCacheLookup(
          cid, token_pos, type_class,
          is_instance_lbl, is_not_instance_lbl);
    }
  } else {
    return GenerateUninstantiatedTypeTest(cid,
                                          token_pos,
                                          type,
                                          is_instance_lbl,
                                          is_not_instance_lbl);
  }
  return SubtypeTestCache::null();
}


// If instanceof type test cannot be performed successfully at compile time and
// therefore eliminated, optimize it by adding inlined tests for:
// - NULL -> return false.
// - Smi -> compile time subtype check (only if dst class is not parameterized).
// - Class equality (only if class is not parameterized).
// Inputs:
// - RAX: object.
// - RDX: instantiator type arguments or raw_null.
// - RCX: instantiator or raw_null.
// Clobbers RCX and RDX.
// Returns:
// - true or false in RAX.
void FlowGraphCompiler::GenerateInstanceOf(intptr_t cid,
                                           intptr_t token_pos,
                                           intptr_t try_index,
                                           const AbstractType& type,
                                           bool negate_result) {
  ASSERT(type.IsFinalized() && !type.IsMalformed());

  const Immediate raw_null =
      Immediate(reinterpret_cast<intptr_t>(Object::null()));
  Label is_instance, is_not_instance;
  __ pushq(RCX);  // Store instantiator on stack.
  __ pushq(RDX);  // Store instantiator type arguments.
  // If type is instantiated and non-parameterized, we can inline code
  // checking whether the tested instance is a Smi.
  if (type.IsInstantiated()) {
    // A null object is only an instance of Object and Dynamic, which has
    // already been checked above (if the type is instantiated). So we can
    // return false here if the instance is null (and if the type is
    // instantiated).
    // We can only inline this null check if the type is instantiated at compile
    // time, since an uninstantiated type at compile time could be Object or
    // Dynamic at run time.
    __ cmpq(RAX, raw_null);
    __ j(EQUAL, &is_not_instance);
  }

  // Generate inline instanceof test.
  SubtypeTestCache& test_cache = SubtypeTestCache::ZoneHandle();
  test_cache = GenerateInlineInstanceof(cid, token_pos, type,
                                        &is_instance, &is_not_instance);

  // Generate runtime call.
  __ movq(RDX, Address(RSP, 0));  // Get instantiator type arguments.
  __ movq(RCX, Address(RSP, kWordSize));  // Get instantiator.
  __ PushObject(Object::ZoneHandle());  // Make room for the result.
  __ pushq(Immediate(Smi::RawValue(token_pos)));  // Source location.
  __ pushq(Immediate(Smi::RawValue(cid)));  // Computation id.
  __ pushq(RAX);  // Push the instance.
  __ PushObject(type);  // Push the type.
  __ pushq(RCX);  // TODO(srdjan): Pass instantiator instead of null.
  __ pushq(RDX);  // Instantiator type arguments.
  __ LoadObject(RAX, test_cache);
  __ pushq(RAX);
  GenerateCallRuntime(cid, token_pos, try_index, kInstanceofRuntimeEntry);
  // Pop the two parameters supplied to the runtime entry. The result of the
  // instanceof runtime call will be left as the result of the operation.
  __ Drop(7);
  Label done;
  if (negate_result) {
    __ popq(RDX);
    __ LoadObject(RAX, bool_true());
    __ cmpq(RDX, RAX);
    __ j(NOT_EQUAL, &done, Assembler::kNearJump);
    __ LoadObject(RAX, bool_false());
  } else {
    __ popq(RAX);
  }
  __ jmp(&done, Assembler::kNearJump);

  __ Bind(&is_not_instance);
  __ LoadObject(RAX, negate_result ? bool_true() : bool_false());
  __ jmp(&done, Assembler::kNearJump);

  __ Bind(&is_instance);
  __ LoadObject(RAX, negate_result ? bool_false() : bool_true());
  __ Bind(&done);
  __ popq(RDX);  // Remove pushed instantiator type arguments.
  __ popq(RCX);  // Remove pushed instantiator.
}


// Optimize assignable type check by adding inlined tests for:
// - NULL -> return NULL.
// - Smi -> compile time subtype check (only if dst class is not parameterized).
// - Class equality (only if class is not parameterized).
// Inputs:
// - RAX: object.
// - RDX: instantiator type arguments or raw_null.
// - RCX: instantiator or raw_null.
// Returns:
// - object in RAX for successful assignable check (or throws TypeError).
// Performance notes: positive checks must be quick, negative checks can be slow
// as they throw an exception.
void FlowGraphCompiler::GenerateAssertAssignable(intptr_t cid,
                                                 intptr_t token_pos,
                                                 intptr_t try_index,
                                                 const AbstractType& dst_type,
                                                 const String& dst_name) {
  ASSERT(token_pos >= 0);
  ASSERT(!dst_type.IsNull());
  ASSERT(dst_type.IsFinalized());
  // Assignable check is skipped in FlowGraphBuilder, not here.
  ASSERT(dst_type.IsMalformed() ||
         (!dst_type.IsDynamicType() && !dst_type.IsObjectType()));
  ASSERT(!dst_type.IsVoidType());
  __ pushq(RCX);  // Store instantiator.
  __ pushq(RDX);  // Store instantiator type arguments.
  // A null object is always assignable and is returned as result.
  const Immediate raw_null =
      Immediate(reinterpret_cast<intptr_t>(Object::null()));
  Label is_assignable, runtime_call;
  __ cmpq(RAX, raw_null);
  __ j(EQUAL, &is_assignable);

  // Generate throw new TypeError() if the type is malformed.
  if (dst_type.IsMalformed()) {
    const Error& error = Error::Handle(dst_type.malformed_error());
    const String& error_message = String::ZoneHandle(
        String::NewSymbol(error.ToErrorCString()));
    __ PushObject(Object::ZoneHandle());  // Make room for the result.
    __ pushq(Immediate(Smi::RawValue(token_pos)));  // Source location.
    __ pushq(RAX);  // Push the source object.
    __ PushObject(dst_name);  // Push the name of the destination.
    __ PushObject(error_message);
    GenerateCallRuntime(cid,
                        token_pos,
                        try_index,
                        kMalformedTypeErrorRuntimeEntry);
    // We should never return here.
    __ int3();

    __ Bind(&is_assignable);  // For a null object.
    return;
  }

  // Generate inline type check, linking to runtime call if not assignable.
  SubtypeTestCache& test_cache = SubtypeTestCache::ZoneHandle();
  test_cache = GenerateInlineInstanceof(cid, token_pos, dst_type,
                                        &is_assignable, &runtime_call);

  __ Bind(&runtime_call);
  __ movq(RDX, Address(RSP, 0));  // Get instantiator type arguments.
  __ movq(RCX, Address(RSP, kWordSize));  // Get instantiator.
  __ PushObject(Object::ZoneHandle());  // Make room for the result.
  __ pushq(Immediate(Smi::RawValue(token_pos)));  // Source location.
  __ pushq(Immediate(Smi::RawValue(cid)));  // Computation id.
  __ pushq(RAX);  // Push the source object.
  __ PushObject(dst_type);  // Push the type of the destination.
  __ pushq(RCX);  // Instantiator.
  __ pushq(RDX);  // Instantiator type arguments.
  __ PushObject(dst_name);  // Push the name of the destination.
  __ LoadObject(RAX, test_cache);
  __ pushq(RAX);
  GenerateCallRuntime(cid,
                      token_pos,
                      try_index,
                      kTypeCheckRuntimeEntry);
  // Pop the parameters supplied to the runtime entry. The result of the
  // type check runtime call is the checked value.
  __ Drop(8);
  __ popq(RAX);

  __ Bind(&is_assignable);
  __ popq(RDX);  // Remove pushed instantiator type arguments..
  __ popq(RCX);  // Remove pushed instantiator.
}


void FlowGraphCompiler::EmitInstructionPrologue(Instruction* instr) {
  LocationSummary* locs = instr->locs();
  ASSERT(locs != NULL);

  frame_register_allocator()->AllocateRegisters(instr);

  // TODO(vegorov): adjust assertion when we start removing comparison from the
  // graph when it is merged with a branch.
  ASSERT(locs->is_call() ||
         (instr->IsBranch() && instr->AsBranch()->is_fused_with_comparison()) ||
         (locs->input_count() == instr->InputCount()));
}


void FlowGraphCompiler::CopyParameters() {
  const Function& function = parsed_function().function();
  const bool is_native_instance_closure =
      function.is_native() && function.IsImplicitInstanceClosureFunction();
  LocalScope* scope = parsed_function().node_sequence()->scope();
  const int num_fixed_params = function.num_fixed_parameters();
  const int num_opt_params = function.num_optional_parameters();
  int implicit_this_param_pos = is_native_instance_closure ? -1 : 0;
  ASSERT(parsed_function().first_parameter_index() ==
         ParsedFunction::kFirstLocalSlotIndex + implicit_this_param_pos);
  // Copy positional arguments.
  // Check that no fewer than num_fixed_params positional arguments are passed
  // in and that no more than num_params arguments are passed in.
  // Passed argument i at fp[1 + argc - i]
  // copied to fp[ParsedFunction::kFirstLocalSlotIndex - i].
  const int num_params = num_fixed_params + num_opt_params;

  // Total number of args is the first Smi in args descriptor array (R10).
  __ movq(RBX, FieldAddress(R10, Array::data_offset()));
  // Check that num_args <= num_params.
  Label wrong_num_arguments;
  __ cmpq(RBX, Immediate(Smi::RawValue(num_params)));
  __ j(GREATER, &wrong_num_arguments);
  // Number of positional args is the second Smi in descriptor array (R10).
  __ movq(RCX, FieldAddress(R10, Array::data_offset() + (1 * kWordSize)));
  // Check that num_pos_args >= num_fixed_params.
  __ cmpq(RCX, Immediate(Smi::RawValue(num_fixed_params)));
  __ j(LESS, &wrong_num_arguments);

  // Since RBX and RCX are Smi, use TIMES_4 instead of TIMES_8.
  // Let RBX point to the last passed positional argument, i.e. to
  // fp[1 + num_args - (num_pos_args - 1)].
  __ subq(RBX, RCX);
  __ leaq(RBX, Address(RBP, RBX, TIMES_4, 2 * kWordSize));

  // Let RDI point to the last copied positional argument, i.e. to
  // fp[ParsedFunction::kFirstLocalSlotIndex - (num_pos_args - 1)].
  const int index =
      ParsedFunction::kFirstLocalSlotIndex + 1 + implicit_this_param_pos;
  // First copy captured receiver if function is an implicit native closure.
  if (is_native_instance_closure) {
    __ movq(RAX, FieldAddress(CTX, Context::variable_offset(0)));
    __ movq(Address(RBP, (index * kWordSize)), RAX);
  }
  __ SmiUntag(RCX);
  __ movq(RAX, RCX);
  __ negq(RAX);
  // -num_pos_args is in RAX.
  // (ParsedFunction::kFirstLocalSlotIndex + 1 + implicit_this_param_pos)
  // is in index.
  __ leaq(RDI, Address(RBP, RAX, TIMES_8, (index * kWordSize)));
  Label loop, loop_condition;
  __ jmp(&loop_condition, Assembler::kNearJump);
  // We do not use the final allocation index of the variable here, i.e.
  // scope->VariableAt(i)->index(), because captured variables still need
  // to be copied to the context that is not yet allocated.
  const Address argument_addr(RBX, RCX, TIMES_8, 0);
  const Address copy_addr(RDI, RCX, TIMES_8, 0);
  __ Bind(&loop);
  __ movq(RAX, argument_addr);
  __ movq(copy_addr, RAX);
  __ Bind(&loop_condition);
  __ decq(RCX);
  __ j(POSITIVE, &loop, Assembler::kNearJump);

  // Copy or initialize optional named arguments.
  const Immediate raw_null =
      Immediate(reinterpret_cast<intptr_t>(Object::null()));
  Label all_arguments_processed;
  if (num_opt_params > 0) {
    // Start by alphabetically sorting the names of the optional parameters.
    LocalVariable** opt_param = new LocalVariable*[num_opt_params];
    int* opt_param_position = new int[num_opt_params];
    for (int pos = num_fixed_params; pos < num_params; pos++) {
      LocalVariable* parameter = scope->VariableAt(pos);
      const String& opt_param_name = parameter->name();
      int i = pos - num_fixed_params;
      while (--i >= 0) {
        LocalVariable* param_i = opt_param[i];
        const intptr_t result = opt_param_name.CompareTo(param_i->name());
        ASSERT(result != 0);
        if (result > 0) break;
        opt_param[i + 1] = opt_param[i];
        opt_param_position[i + 1] = opt_param_position[i];
      }
      opt_param[i + 1] = parameter;
      opt_param_position[i + 1] = pos;
    }
    // Generate code handling each optional parameter in alphabetical order.
    // Total number of args is the first Smi in args descriptor array (R10).
    __ movq(RBX, FieldAddress(R10, Array::data_offset()));
    // Number of positional args is the second Smi in descriptor array (R10).
    __ movq(RCX, FieldAddress(R10, Array::data_offset() + (1 * kWordSize)));
    __ SmiUntag(RCX);
    // Let RBX point to the first passed argument, i.e. to fp[1 + argc - 0].
    __ leaq(RBX, Address(RBP, RBX, TIMES_4, kWordSize));  // RBX is Smi.
    // Let EDI point to the name/pos pair of the first named argument.
    __ leaq(RDI, FieldAddress(R10, Array::data_offset() + (2 * kWordSize)));
    for (int i = 0; i < num_opt_params; i++) {
      // Handle this optional parameter only if k or fewer positional arguments
      // have been passed, where k is the position of this optional parameter in
      // the formal parameter list.
      Label load_default_value, assign_optional_parameter, next_parameter;
      const int param_pos = opt_param_position[i];
      __ cmpq(RCX, Immediate(param_pos));
      __ j(GREATER, &next_parameter, Assembler::kNearJump);
      // Check if this named parameter was passed in.
      __ movq(RAX, Address(RDI, 0));  // Load RAX with the name of the argument.
      __ CompareObject(RAX, opt_param[i]->name());
      __ j(NOT_EQUAL, &load_default_value, Assembler::kNearJump);
      // Load RAX with passed-in argument at provided arg_pos, i.e. at
      // fp[1 + argc - arg_pos].
      __ movq(RAX, Address(RDI, kWordSize));  // RAX is arg_pos as Smi.
      __ addq(RDI, Immediate(2 * kWordSize));  // Point to next name/pos pair.
      __ negq(RAX);
      Address argument_addr(RBX, RAX, TIMES_4, 0);  // RAX is a negative Smi.
      __ movq(RAX, argument_addr);
      __ jmp(&assign_optional_parameter, Assembler::kNearJump);
      __ Bind(&load_default_value);
      // Load RAX with default argument at pos.
      const Object& value = Object::ZoneHandle(
          parsed_function().default_parameter_values().At(
              param_pos - num_fixed_params));
      __ LoadObject(RAX, value);
      __ Bind(&assign_optional_parameter);
      // Assign RAX to fp[ParsedFunction::kFirstLocalSlotIndex - param_pos].
      // We do not use the final allocation index of the variable here, i.e.
      // scope->VariableAt(i)->index(), because captured variables still need
      // to be copied to the context that is not yet allocated.
      intptr_t computed_param_pos = (ParsedFunction::kFirstLocalSlotIndex -
                                     param_pos + implicit_this_param_pos);
      const Address param_addr(RBP, (computed_param_pos * kWordSize));
      __ movq(param_addr, RAX);
      __ Bind(&next_parameter);
    }
    delete[] opt_param;
    delete[] opt_param_position;
    // Check that RDI now points to the null terminator in the array descriptor.
    __ cmpq(Address(RDI, 0), raw_null);
    __ j(EQUAL, &all_arguments_processed, Assembler::kNearJump);
  } else {
    ASSERT(is_native_instance_closure);
    __ jmp(&all_arguments_processed, Assembler::kNearJump);
  }

  __ Bind(&wrong_num_arguments);
  if (StackSize() != 0) {
    // We need to unwind the space we reserved for locals and copied parameters.
    // The NoSuchMethodFunction stub does not expect to see that area on the
    // stack.
    __ addq(RSP, Immediate(StackSize() * kWordSize));
  }
  if (function.IsClosureFunction()) {
    GenerateCallRuntime(AstNode::kNoId,
                        0,
                        CatchClauseNode::kInvalidTryIndex,
                        kClosureArgumentMismatchRuntimeEntry);
  } else {
    ASSERT(!IsLeaf());
    // Invoke noSuchMethod function.
    const int kNumArgsChecked = 1;
    ICData& ic_data = ICData::ZoneHandle();
    ic_data = ICData::New(function,
                          String::Handle(function.name()),
                          AstNode::kNoId,
                          kNumArgsChecked);
    __ LoadObject(RBX, ic_data);
    // RBP - 8 : PC marker, allows easy identification of RawInstruction obj.
    // RBP : points to previous frame pointer.
    // RBP + 8 : points to return address.
    // RBP + 16 : address of last argument (arg n-1).
    // RSP + 16 + 8*(n-1) : address of first argument (arg 0).
    // RBX : ic-data.
    // R10 : arguments descriptor array.
    __ call(&StubCode::CallNoSuchMethodFunctionLabel());
  }

  if (FLAG_trace_functions) {
    __ pushq(RAX);  // Preserve result.
    __ PushObject(Function::ZoneHandle(function.raw()));
    GenerateCallRuntime(AstNode::kNoId,
                        0,
                        CatchClauseNode::kInvalidTryIndex,
                        kTraceFunctionExitRuntimeEntry);
    __ popq(RAX);  // Remove argument.
    __ popq(RAX);  // Restore result.
  }
  __ LeaveFrame();
  __ ret();

  __ Bind(&all_arguments_processed);
  // Nullify originally passed arguments only after they have been copied and
  // checked, otherwise noSuchMethod would not see their original values.
  // This step can be skipped in case we decide that formal parameters are
  // implicitly final, since garbage collecting the unmodified value is not
  // an issue anymore.

  // R10 : arguments descriptor array.
  // Total number of args is the first Smi in args descriptor array (R10).
  __ movq(RCX, FieldAddress(R10, Array::data_offset()));
  __ SmiUntag(RCX);
  Label null_args_loop, null_args_loop_condition;
  __ jmp(&null_args_loop_condition, Assembler::kNearJump);
  const Address original_argument_addr(RBP, RCX, TIMES_8, 2 * kWordSize);
  __ Bind(&null_args_loop);
  __ movq(original_argument_addr, raw_null);
  __ Bind(&null_args_loop_condition);
  __ decq(RCX);
  __ j(POSITIVE, &null_args_loop, Assembler::kNearJump);
}


void FlowGraphCompiler::GenerateInlinedGetter(intptr_t offset) {
  // TOS: return address.
  // +1 : receiver.
  // Sequence node has one return node, its input is load field node.
  __ movq(RAX, Address(RSP, 1 * kWordSize));
  __ movq(RAX, FieldAddress(RAX, offset));
  __ ret();
}


void FlowGraphCompiler::GenerateInlinedSetter(intptr_t offset) {
  // TOS: return address.
  // +1 : value
  // +2 : receiver.
  // Sequence node has one store node and one return NULL node.
  __ movq(RAX, Address(RSP, 2 * kWordSize));  // Receiver.
  __ movq(RBX, Address(RSP, 1 * kWordSize));  // Value.
  __ StoreIntoObject(RAX, FieldAddress(RAX, offset), RBX);
  const Immediate raw_null =
      Immediate(reinterpret_cast<intptr_t>(Object::null()));
  __ movq(RAX, raw_null);
  __ ret();
}


void FlowGraphCompiler::GenerateInlinedMathSqrt(Label* done) {
  Label smi_to_double, double_op, call_method;
  __ movq(RAX, Address(RSP, 0));
  __ testq(RAX, Immediate(kSmiTagMask));
  __ j(ZERO, &smi_to_double);
  __ CompareClassId(RAX, kDouble);
  __ j(NOT_EQUAL, &call_method);
  __ movsd(XMM1, FieldAddress(RAX, Double::value_offset()));
  __ Bind(&double_op);
  __ sqrtsd(XMM0, XMM1);
  AssemblerMacros::TryAllocate(assembler_,
                               double_class_,
                               &call_method,
                               RAX);  // Result register.
  __ movsd(FieldAddress(RAX, Double::value_offset()), XMM0);
  __ Drop(1);
  __ jmp(done);
  __ Bind(&smi_to_double);
  __ SmiUntag(RAX);
  __ cvtsi2sd(XMM1, RAX);
  __ jmp(&double_op);
  __ Bind(&call_method);
}


void FlowGraphCompiler::CompileGraph() {
  InitCompiler();
  if (TryIntrinsify()) {
    // Although this intrinsified code will never be patched, it must satisfy
    // CodePatcher::CodeIsPatchable, which verifies that this code has a minimum
    // code size, and nop(2) increases the minimum code size appropriately.
    __ nop(2);
    __ int3();
    __ jmp(&StubCode::FixCallersTargetLabel());
    return;
  }
  // Specialized version of entry code from CodeGenerator::GenerateEntryCode.
  const Function& function = parsed_function().function();

  const int parameter_count = function.num_fixed_parameters();
  const int num_copied_params = parsed_function().copied_parameter_count();
  const int local_count = parsed_function().stack_local_count();
  if (IsLeaf()) {
    AssemblerMacros::EnterDartLeafFrame(assembler(), (StackSize() * kWordSize));
  } else {
    AssemblerMacros::EnterDartFrame(assembler(), (StackSize() * kWordSize));
  }
  // We check the number of passed arguments when we have to copy them due to
  // the presence of optional named parameters.
  // No such checking code is generated if only fixed parameters are declared,
  // unless we are debug mode or unless we are compiling a closure.
  if (num_copied_params == 0) {
#ifdef DEBUG
    const bool check_arguments = true;
#else
    const bool check_arguments = function.IsClosureFunction();
#endif
    if (check_arguments) {
      // Check that num_fixed <= argc <= num_params.
      Label argc_in_range;
      // Total number of args is the first Smi in args descriptor array (R10).
      __ movq(RAX, FieldAddress(R10, Array::data_offset()));
      __ cmpq(RAX, Immediate(Smi::RawValue(parameter_count)));
      __ j(EQUAL, &argc_in_range, Assembler::kNearJump);
      if (function.IsClosureFunction()) {
        GenerateCallRuntime(AstNode::kNoId,
                            function.token_pos(),
                            CatchClauseNode::kInvalidTryIndex,
                            kClosureArgumentMismatchRuntimeEntry);
      } else {
        __ Stop("Wrong number of arguments");
      }
      __ Bind(&argc_in_range);
    }
  } else {
    CopyParameters();
  }
  // Initialize (non-argument) stack allocated locals to null.
  if (local_count > 0) {
    const Immediate raw_null =
        Immediate(reinterpret_cast<intptr_t>(Object::null()));
    __ movq(RAX, raw_null);
    const int base = parsed_function().first_stack_local_index();
    for (int i = 0; i < local_count; ++i) {
      // Subtract index i (locals lie at lower addresses than RBP).
      __ movq(Address(RBP, (base - i) * kWordSize), RAX);
    }
  }

  if (!IsLeaf()) {
    // Generate stack overflow check.
    __ movq(RDI, Immediate(Isolate::Current()->stack_limit_address()));
    __ cmpq(RSP, Address(RDI, 0));
    Label no_stack_overflow;
    __ j(ABOVE, &no_stack_overflow, Assembler::kNearJump);
    GenerateCallRuntime(AstNode::kNoId,
                        function.token_pos(),
                        CatchClauseNode::kInvalidTryIndex,
                        kStackOverflowRuntimeEntry);
    __ Bind(&no_stack_overflow);
  }
  if (FLAG_print_scopes) {
    // Print the function scope (again) after generating the prologue in order
    // to see annotations such as allocation indices of locals.
    if (FLAG_print_ast) {
      // Second printing.
      OS::Print("Annotated ");
    }
    AstPrinter::PrintFunctionScope(parsed_function());
  }

  VisitBlocks();

  __ int3();
  GenerateDeferredCode();
  // Emit function patching code. This will be swapped with the first 13 bytes
  // at entry point.
  pc_descriptors_list()->AddDescriptor(PcDescriptors::kPatchCode,
                                       assembler()->CodeSize(),
                                       AstNode::kNoId,
                                       0,
                                       -1);
  __ jmp(&StubCode::FixCallersTargetLabel());
}


void FlowGraphCompiler::GenerateCall(intptr_t token_pos,
                                     intptr_t try_index,
                                     const ExternalLabel* label,
                                     PcDescriptors::Kind kind) {
  ASSERT(!IsLeaf());
  ASSERT(frame_register_allocator()->IsSpilled());
  __ call(label);
  AddCurrentDescriptor(kind, AstNode::kNoId, token_pos, try_index);
}


void FlowGraphCompiler::GenerateCallRuntime(intptr_t cid,
                                            intptr_t token_pos,
                                            intptr_t try_index,
                                            const RuntimeEntry& entry) {
  ASSERT(!IsLeaf());
  ASSERT(frame_register_allocator()->IsSpilled());
  __ CallRuntime(entry);
  AddCurrentDescriptor(PcDescriptors::kOther, cid, token_pos, try_index);
}


intptr_t FlowGraphCompiler::EmitInstanceCall(ExternalLabel* target_label,
                                             const ICData& ic_data,
                                             const Array& arguments_descriptor,
                                             intptr_t argument_count) {
  ASSERT(!IsLeaf());
  __ LoadObject(RBX, ic_data);
  __ LoadObject(R10, arguments_descriptor);

  __ call(target_label);
  const intptr_t descr_offset = assembler()->CodeSize();
  __ Drop(argument_count);
  return descr_offset;
}


intptr_t FlowGraphCompiler::EmitStaticCall(const Function& function,
                                           const Array& arguments_descriptor,
                                           intptr_t argument_count) {
  ASSERT(!IsLeaf());
  __ LoadObject(RBX, function);
  __ LoadObject(R10, arguments_descriptor);
  __ call(&StubCode::CallStaticFunctionLabel());
  const intptr_t descr_offset = assembler()->CodeSize();
  __ Drop(argument_count);
  return descr_offset;
}


// Checks class id of instance against all 'class_ids'. Jump to 'deopt' label
// if no match or instance is Smi.
void FlowGraphCompiler::EmitClassChecksNoSmi(const ICData& ic_data,
                                             Register instance_reg,
                                             Register temp_reg,
                                             Label* deopt) {
  Label ok;
  ASSERT(ic_data.GetReceiverClassIdAt(0) != kSmi);
  __ testq(instance_reg, Immediate(kSmiTagMask));
  __ j(ZERO, deopt);
  Label is_ok;
  const intptr_t num_checks = ic_data.NumberOfChecks();
  const bool use_near_jump = num_checks < 5;
  __ LoadClassId(temp_reg, instance_reg);
  for (intptr_t i = 0; i < num_checks; i++) {
    __ cmpl(temp_reg, Immediate(ic_data.GetReceiverClassIdAt(i)));
    if (i == (num_checks - 1)) {
      __ j(NOT_EQUAL, deopt);
    } else {
      if (use_near_jump) {
        __ j(EQUAL, &is_ok, Assembler::kNearJump);
      } else {
        __ j(EQUAL, &is_ok);
      }
    }
  }
  __ Bind(&is_ok);
}


void FlowGraphCompiler::LoadDoubleOrSmiToXmm(XmmRegister result,
                                             Register reg,
                                             Register temp,
                                             Label* not_double_or_smi) {
  Label is_smi, done;
  __ testq(reg, Immediate(kSmiTagMask));
  __ j(ZERO, &is_smi);
  __ CompareClassId(reg, kDouble);
  __ j(NOT_EQUAL, not_double_or_smi);
  __ movsd(result, FieldAddress(reg, Double::value_offset()));
  __ jmp(&done);
  __ Bind(&is_smi);
  __ movq(temp, reg);
  __ SmiUntag(temp);
  __ cvtsi2sd(result, temp);
  __ Bind(&done);
}


#undef __

}  // namespace dart

#endif  // defined TARGET_ARCH_X64
