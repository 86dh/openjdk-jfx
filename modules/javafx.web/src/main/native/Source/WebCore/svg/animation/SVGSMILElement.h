/*
 * Copyright (C) 2008-2024 Apple Inc. All rights reserved.
 * Copyright (C) 2013-2020 Google Inc. All rights reserved.
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

#pragma once

#include "SMILTime.h"
#include "SVGElement.h"
#include <wtf/HashSet.h>

namespace WebCore {

class ConditionEventListener;
class SMILTimeContainer;
class SVGSMILElement;

template<typename T, typename Counter> class EventSender;

using SMILEventSender = EventSender<SVGSMILElement, WeakPtrImplWithEventTargetData>;

// This class implements SMIL interval timing model as needed for SVG animation.
class SVGSMILElement : public SVGElement {
    WTF_MAKE_TZONE_OR_ISO_ALLOCATED(SVGSMILElement);
    WTF_OVERRIDE_DELETE_FOR_CHECKED_PTR(SVGSMILElement);
public:
    SVGSMILElement(const QualifiedName&, Document&, UniqueRef<SVGPropertyRegistry>&&);
    virtual ~SVGSMILElement();

    void attributeChanged(const QualifiedName&, const AtomString& oldValue, const AtomString& newValue, AttributeModificationReason) override;
    void svgAttributeChanged(const QualifiedName&) override;
    InsertedIntoAncestorResult insertedIntoAncestor(InsertionType, ContainerNode&) override;
    void removedFromAncestor(RemovalType, ContainerNode&) override;

    virtual bool hasValidAttributeType() const = 0;
    virtual bool hasValidAttributeName() const;
    virtual void animationAttributeChanged() = 0;

    SMILTimeContainer* timeContainer() { return m_timeContainer.get(); }
    RefPtr<SMILTimeContainer> protectedTimeContainer() const;

    SVGElement* targetElement() const { return m_targetElement.get(); }
    RefPtr<SVGElement> protectedTargetElement() const { return m_targetElement.get(); }
    const QualifiedName& attributeName() const { return m_attributeName; }

    void beginByLinkActivation();

    enum Restart { RestartAlways, RestartWhenNotActive, RestartNever };
    Restart restart() const;

    enum FillMode { FillRemove, FillFreeze };
    FillMode fill() const;

    SMILTime dur() const;
    SMILTime repeatDur() const;
    SMILTime repeatCount() const;
    SMILTime maxValue() const;
    SMILTime minValue() const;

    SMILTime elapsed() const;

    SMILTime intervalBegin() const { return m_intervalBegin; }
    SMILTime previousIntervalBegin() const { return m_previousIntervalBegin; }
    SMILTime simpleDuration() const;

    void seekToIntervalCorrespondingToTime(SMILTime elapsed);
    bool progress(SMILTime elapsed, SVGSMILElement& firstAnimation, bool seekToTime);
    SMILTime nextProgressTime() const;

    void reset();

    static SMILTime parseClockValue(StringView);
    static SMILTime parseOffsetValue(StringView);

    bool isContributing(SMILTime elapsed) const;
    bool isFrozen() const;

    unsigned documentOrderIndex() const { return m_documentOrderIndex; }
    void setDocumentOrderIndex(unsigned index) { m_documentOrderIndex = index; }

    virtual bool isAdditive() const = 0;
    virtual void startAnimation() = 0;
    virtual void stopAnimation(SVGElement* targetElement) = 0;
    virtual void applyResultsToTarget() = 0;

    void connectConditions();
    bool hasConditionsConnected() const { return m_conditionsConnected; }

    void dispatchPendingEvent(SMILEventSender*, const AtomString& eventType);

protected:
    enum ActiveState { Inactive, Active, Frozen };
    ActiveState activeState() const { return m_activeState; }
    void setInactive() { m_activeState = Inactive; }

    bool rendererIsNeeded(const RenderStyle&) override { return false; }

    // Sub-classes may need to take action when the target is changed.
    virtual void setTargetElement(SVGElement*);
    virtual void setAttributeName(const QualifiedName&);

    void didFinishInsertingNode() override;

    enum BeginOrEnd { Begin, End };

    void addInstanceTime(BeginOrEnd, SMILTime, SMILTimeWithOrigin::Origin = SMILTimeWithOrigin::ParserOrigin);

private:
    void buildPendingResource() override;
    void clearResourceReferences();

    void clearTarget() override;

    virtual void startedActiveInterval() = 0;
    void endedActiveInterval();
    virtual void updateAnimation(float percent, unsigned repeat) = 0;

    static bool isSupportedAttribute(const QualifiedName&);
    bool hasPresentationalHintsForAttribute(const QualifiedName&) const override;
    QualifiedName constructAttributeName() const;
    void updateAttributeName();

    SMILTime findInstanceTime(BeginOrEnd, SMILTime minimumTime, bool equalsMinimumOK) const;
    void resolveFirstInterval();
    bool resolveNextInterval();
    void resolveInterval(bool first, SMILTime& beginResult, SMILTime& endResult) const;
    SMILTime resolveActiveEnd(SMILTime resolvedBegin, SMILTime resolvedEnd) const;
    SMILTime repeatingDuration() const;
    void checkRestart(SMILTime elapsed);
    void beginListChanged(SMILTime eventTime);
    void endListChanged(SMILTime eventTime);

    // This represents conditions on elements begin or end list that need to be resolved on runtime
    // for example <animate begin="otherElement.begin + 8s; button.click" ... />
    struct Condition {
        enum Type { EventBase, Syncbase, AccessKey };
        Condition(Type, BeginOrEnd, const String& baseID, const AtomString& name, SMILTime offset, int repeats = -1);
        Type m_type;
        BeginOrEnd m_beginOrEnd;
        String m_baseID;
        AtomString m_name;
        SMILTime m_offset;
        int m_repeats { -1 };
        RefPtr<Element> m_syncbase;
        RefPtr<ConditionEventListener> m_eventListener;
    };
    bool parseCondition(StringView, BeginOrEnd);
    void parseBeginOrEnd(StringView, BeginOrEnd);
    RefPtr<Element> eventBaseFor(const Condition&);

    void disconnectConditions();

    void notifyDependentsIntervalChanged();
    void createInstanceTimesFromSyncbase(SVGSMILElement* syncbase);
    void addTimeDependent(SVGSMILElement*);
    void removeTimeDependent(SVGSMILElement*);

    ActiveState determineActiveState(SMILTime elapsed) const;
    float calculateAnimationPercentAndRepeat(SMILTime elapsed, unsigned& repeat) const;
    SMILTime calculateNextProgressTime(SMILTime elapsed) const;

    bool isSMILElement() const final { return true; }

    QualifiedName m_attributeName;

    WeakPtr<SVGElement, WeakPtrImplWithEventTargetData> m_targetElement;

    Vector<Condition> m_conditions;
    bool m_conditionsConnected;
    bool m_hasEndEventConditions;

    bool m_isWaitingForFirstInterval;

    WeakHashSet<SVGSMILElement, WeakPtrImplWithEventTargetData> m_timeDependents;

    // Instance time lists
    Vector<SMILTimeWithOrigin> m_beginTimes;
    Vector<SMILTimeWithOrigin> m_endTimes;

    // This is the upcoming or current interval
    SMILTime m_intervalBegin;
    SMILTime m_intervalEnd;

    SMILTime m_previousIntervalBegin;

    ActiveState m_activeState;
    float m_lastPercent;
    unsigned m_lastRepeat;

    SMILTime m_nextProgressTime;

    RefPtr<SMILTimeContainer> m_timeContainer;
    unsigned m_documentOrderIndex;

    mutable SMILTime m_cachedDur;
    mutable SMILTime m_cachedRepeatDur;
    mutable SMILTime m_cachedRepeatCount;
    mutable SMILTime m_cachedMin;
    mutable SMILTime m_cachedMax;

    friend class ConditionEventListener;
};

} // namespace WebCore

SPECIALIZE_TYPE_TRAITS_BEGIN(WebCore::SVGSMILElement)
    static bool isType(const WebCore::SVGElement& element) { return element.isSMILElement(); }
    static bool isType(const WebCore::Node& node)
    {
        auto* svgElement = dynamicDowncast<WebCore::SVGElement>(node);
        return svgElement && isType(*svgElement);
    }
SPECIALIZE_TYPE_TRAITS_END()
