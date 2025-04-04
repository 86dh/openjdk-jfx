/*
 * Copyright (C) 2007-2021 Apple Inc. All rights reserved.
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

#include "CachedResource.h"
#include "CachedResourceClient.h"
#include "Font.h"
#include "FrameLoaderTypes.h"
#include "TextFlags.h"
#include "TrustedFonts.h"
#include <pal/SessionID.h>

namespace WebCore {

class CachedResourceLoader;
class FontCreationContext;
class FontDescription;
class FontPlatformData;
struct FontSelectionSpecifiedCapabilities;
class SVGDocument;
class SVGFontElement;
struct FontCustomPlatformData;

template <typename T> class FontTaggedSettings;
typedef FontTaggedSettings<int> FontFeatureSettings;

class CachedFont : public CachedResource {
public:
    CachedFont(CachedResourceRequest&&, PAL::SessionID, const CookieJar*, Type = Type::FontResource);
    virtual ~CachedFont();

    void beginLoadIfNeeded(CachedResourceLoader&);
    bool stillNeedsLoad() const override { return !m_loadInitiated; }

    virtual bool ensureCustomFontData();
    static RefPtr<FontCustomPlatformData> createCustomFontData(SharedBuffer&, const String& itemInCollection, bool& wrapping);
    static RefPtr<FontCustomPlatformData> createCustomFontDataExperimentalParser(SharedBuffer&, const String& itemInCollection, bool& wrapping);
    static FontPlatformData platformDataFromCustomData(FontCustomPlatformData&, const FontDescription&, bool bold, bool italic, const FontCreationContext&);

    virtual RefPtr<Font> createFont(const FontDescription&, bool syntheticBold, bool syntheticItalic, const FontCreationContext&);

    bool didRefuseToParseCustomFontWithSafeFontParser() const { return m_didRefuseToParseCustomFont; }

protected:
    FontPlatformData platformDataFromCustomData(const FontDescription&, bool bold, bool italic, const FontCreationContext&);

    bool ensureCustomFontData(SharedBuffer* data);

private:
    String calculateItemInCollection() const;

    void checkNotify(const NetworkLoadMetrics&, LoadWillContinueInAnotherProcess = LoadWillContinueInAnotherProcess::No) override;
    bool mayTryReplaceEncodedData() const override;

    void load(CachedResourceLoader&) override;
    NO_RETURN_DUE_TO_ASSERT void setBodyDataFrom(const CachedResource&) final { ASSERT_NOT_REACHED(); }

    void didAddClient(CachedResourceClient&) override;
    void finishLoading(const FragmentedSharedBuffer*, const NetworkLoadMetrics&) override;

    void allClientsRemoved() override;

    FontParsingPolicy policyForCustomFont(const Ref<SharedBuffer>& data);
    void setErrorAndDeleteData();

    bool m_loadInitiated;
    bool m_hasCreatedFontDataWrappingResource;

    FontParsingPolicy m_fontParsingPolicy { FontParsingPolicy::Deny };
    bool m_didRefuseToParseCustomFont { false };

    RefPtr<FontCustomPlatformData> m_fontCustomPlatformData;

    friend class MemoryCache;
};

} // namespace WebCore

SPECIALIZE_TYPE_TRAITS_CACHED_RESOURCE(CachedFont, CachedResource::Type::FontResource)
