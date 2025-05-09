/*
 * Copyright (C) 2015-2023 Apple Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"
#include "B3ValueRep.h"

#if ENABLE(B3_JIT)

#include "AssemblyHelpers.h"
#include "JSCJSValueInlines.h"
#include <wtf/TZoneMallocInlines.h>

namespace JSC { namespace B3 {

WTF_MAKE_TZONE_ALLOCATED_IMPL(ValueRep);

void ValueRep::addUsedRegistersTo(bool isSIMDContext, RegisterSetBuilder& set) const
{
    switch (m_kind) {
    case WarmAny:
    case ColdAny:
    case LateColdAny:
    case SomeRegister:
    case SomeRegisterWithClobber:
    case SomeEarlyRegister:
    case SomeLateRegister:
    case Constant:
        return;
    case LateRegister:
    case Register:
        set.add(reg(), isSIMDContext ? conservativeWidth(reg()) : conservativeWidthWithoutVectors(reg()));
        return;
    case Stack:
    case StackArgument:
        set.add(MacroAssembler::stackPointerRegister, IgnoreVectors);
        set.add(GPRInfo::callFrameRegister, IgnoreVectors);
        return;
#if USE(JSVALUE32_64)
    case SomeRegisterPair:
    case SomeRegisterPairWithClobber:
    case SomeEarlyRegisterPair:
    case SomeLateRegisterPair:
        return;
    case LateRegisterPair:
    case RegisterPair:
        set.add(regLo(), isSIMDContext ? conservativeWidth(regLo()) : conservativeWidthWithoutVectors(reg()));
        set.add(regHi(), isSIMDContext ? conservativeWidth(regHi()) : conservativeWidthWithoutVectors(reg()));
        return;
#endif
    }
    RELEASE_ASSERT_NOT_REACHED();
}

RegisterSetBuilder ValueRep::usedRegisters(bool isSIMDContext) const
{
    RegisterSetBuilder result;
    addUsedRegistersTo(isSIMDContext, result);
    return result;
}

void ValueRep::dump(PrintStream& out) const
{
    out.print(m_kind);
    switch (m_kind) {
    case WarmAny:
    case ColdAny:
    case LateColdAny:
    case SomeRegister:
    case SomeRegisterWithClobber:
    case SomeEarlyRegister:
    case SomeLateRegister:
        return;
    case LateRegister:
    case Register:
        out.print("(", reg(), ")");
        return;
    case Stack:
        out.print("(", offsetFromFP(), ")");
        return;
    case StackArgument:
        out.print("(", offsetFromSP(), ")");
        return;
    case Constant:
        out.print("(", value(), ")");
        return;
#if USE(JSVALUE32_64)
    case SomeRegisterPair:
    case SomeRegisterPairWithClobber:
    case SomeEarlyRegisterPair:
    case SomeLateRegisterPair:
        return;
    case LateRegisterPair:
    case RegisterPair:
        out.print("(", regHi(), ",", regLo(), ")");
        return;
#endif
    }
    RELEASE_ASSERT_NOT_REACHED();
}

// We use `B3::ValueRep` for bookkeeping in the BBQ wasm backend, including on
// 32-bit platforms, but not for code generation (yet!), so we don't actually
// want to provide these symbols until they are properly supported on those
// platforms.

#if USE(JSVALUE64)

void ValueRep::emitRestore(AssemblyHelpers& jit, Reg reg) const
{
    if (reg.isGPR()) {
        switch (kind()) {
        case LateRegister:
        case Register:
            if (isGPR())
                jit.move(gpr(), reg.gpr());
            else
                jit.moveDoubleTo64(fpr(), reg.gpr());
            break;
        case Stack:
            jit.load64(AssemblyHelpers::Address(GPRInfo::callFrameRegister, offsetFromFP()), reg.gpr());
            break;
        case Constant:
            jit.move(AssemblyHelpers::TrustedImm64(value()), reg.gpr());
            break;
        default:
            RELEASE_ASSERT_NOT_REACHED();
            break;
        }
        return;
    }

    switch (kind()) {
    case LateRegister:
    case Register:
        if (isGPR())
            jit.move64ToDouble(gpr(), reg.fpr());
        else
            jit.moveDouble(fpr(), reg.fpr());
        break;
    case Stack:
        jit.loadDouble(AssemblyHelpers::Address(GPRInfo::callFrameRegister, offsetFromFP()), reg.fpr());
        break;
    case Constant:
        jit.move(AssemblyHelpers::TrustedImm64(value()), jit.scratchRegister());
        jit.move64ToDouble(jit.scratchRegister(), reg.fpr());
        break;
    default:
        RELEASE_ASSERT_NOT_REACHED();
        break;
    }
}

ValueRecovery ValueRep::recoveryForJSValue() const
{
    switch (kind()) {
    case LateRegister:
    case Register:
        return ValueRecovery::inGPR(gpr(), DataFormatJS);
    case Stack:
        RELEASE_ASSERT(!(offsetFromFP() % sizeof(EncodedJSValue)));
        return ValueRecovery::displacedInJSStack(
            VirtualRegister(offsetFromFP() / sizeof(EncodedJSValue)),
            DataFormatJS);
    case Constant:
        return ValueRecovery::constant(JSValue::decode(value()));
    default:
        RELEASE_ASSERT_NOT_REACHED();
        return { };
    }
}

#endif // USE(JSVALUE64) [see note above]

} } // namespace JSC::B3

namespace WTF {

using namespace JSC::B3;

void printInternal(PrintStream& out, ValueRep::Kind kind)
{
    switch (kind) {
    case ValueRep::WarmAny:
        out.print("WarmAny");
        return;
    case ValueRep::ColdAny:
        out.print("ColdAny");
        return;
    case ValueRep::LateColdAny:
        out.print("LateColdAny");
        return;
    case ValueRep::SomeRegister:
        out.print("SomeRegister");
        return;
    case ValueRep::SomeRegisterWithClobber:
        out.print("SomeRegisterWithClobber");
        return;
    case ValueRep::SomeEarlyRegister:
        out.print("SomeEarlyRegister");
        return;
    case ValueRep::SomeLateRegister:
        out.print("SomeLateRegister");
        return;
    case ValueRep::Register:
        out.print("Register");
        return;
    case ValueRep::LateRegister:
        out.print("LateRegister");
        return;
    case ValueRep::Stack:
        out.print("Stack");
        return;
    case ValueRep::StackArgument:
        out.print("StackArgument");
        return;
    case ValueRep::Constant:
        out.print("Constant");
        return;
#if USE(JSVALUE32_64)
    case ValueRep::SomeRegisterPair:
        out.print("SomeRegisterPair");
        return;
    case ValueRep::SomeRegisterPairWithClobber:
        out.print("SomeRegisterPairWithClobber");
        return;
    case ValueRep::SomeEarlyRegisterPair:
        out.print("SomeEarlyRegisterPair");
        return;
    case ValueRep::SomeLateRegisterPair:
        out.print("SomeLateRegisterPair");
        return;
    case ValueRep::RegisterPair:
        out.print("RegisterPair");
        return;
    case ValueRep::LateRegisterPair:
        out.print("LateRegisterPair");
        return;
#endif
    }
    RELEASE_ASSERT_NOT_REACHED();
}

} // namespace WTF

#endif // ENABLE(B3_JIT)
