#
# Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# jfxmedia-jni win32/win64 makefile

CC = cl.exe
LINKER = link.exe
MAKEDEPEND = makedepend.exe

BUILD_DIR = $(shell cygpath -u "$(OUTPUT_DIR)/$(BUILD_TYPE)")

SRCBASE_DIR = ../..
OBJBASE_DIR = $(BUILD_DIR)/obj/jfxmedia-jni

GSTREAMER_LITE_DIR = ../../../gstreamer/gstreamer-lite
GLIB_LITE_DIR = ../../../gstreamer/3rd_party/glib
PLUGINS_DIR = ../../../gstreamer/plugins

DIRLIST = jni \
          PipelineManagement \
          MediaManagement \
          Locator \
          Utils \
          Utils/win32 \
          platform/gstreamer

TARGET = $(BUILD_DIR)/$(BASE_NAME).dll

CL_COMPILER_FLAGS = -nologo -W3 -WX- -Gm- -EHsc -GS -fp:precise \
                    -Zc:wchar_t- -Zc:forScope -Gd -errorReport:queue

JNI_INCLUDES =  -I"$(JAVA_HOME)/include" \
                -I"$(JAVA_HOME)/include/win32"

BASE_INCLUDES = -I$(SRCBASE_DIR) \
                -I$(SRCBASE_DIR)/jni

INCLUDES = $(BASE_INCLUDES) \
           $(JNI_INCLUDES) \
           -I$(shell cygpath -ma "$(GENERATED_HEADERS_DIR)") \
           -I$(GLIB_LITE_DIR)/ \
           -I$(GLIB_LITE_DIR)/glib \
           -I$(GLIB_LITE_DIR)/gmodule \
           -I$(GLIB_LITE_DIR)/build/win32/vs100 \
           -I$(GSTREAMER_LITE_DIR)/gstreamer \
           -I$(GSTREAMER_LITE_DIR)/gst-plugins-base/gst-libs \
           -I$(GSTREAMER_LITE_DIR)/gst-plugins-base/win32/common \
           -I$(GSTREAMER_LITE_DIR)/gstreamer/libs \
           -I$(PLUGINS_DIR)

CFLAGS = -DWIN32 \
         -D_WINDOWS \
         -D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR \
         -D_USRDLL \
         -DJFXMEDIA_JNI_EXPORTS \
         -DTARGET_OS_WIN32=1 \
         -D_WIN32_WINNT=0x0500 \
         -DGST_DISABLE_LOADSAVE \
         -DGST_REMOVE_DEPRECATED \
         -DG_DISABLE_DEPRECATED \
         -DGSTREAMER_LITE \
         -D_WINDLL \
         -D_MBCS \
         $(INCLUDES) \
         $(CL_COMPILER_FLAGS)

LIBS = gstreamer-lite.lib \
       glib-lite.lib \
       Winmm.lib \
       kernel32.lib \
       user32.lib \
       comdlg32.lib \
       advapi32.lib

MANIFEST = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).manifest")
PDB      = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).pdb")

LDFLAGS = -dll -out:$(shell cygpath -ma $(TARGET)) -incremental:no -nologo -libpath:$(shell cygpath -ma $(BUILD_DIR)) $(LIBS) \
          -manifest -manifestfile:$(MANIFEST) -manifestuac:"level='asInvoker' uiAccess='false'" \
          -subsystem:windows -dynamicbase -nxcompat -errorreport:queue

ifeq ($(BUILD_TYPE), Release)
    CFLAGS += -DNDEBUG -O2 -MD
    LDFLAGS += -opt:ref -opt:icf
else
    CFLAGS += -D_DEBUG -Od -RTC1 -MDd -Zi -Fd$(PDB)
    LDFLAGS += -debug -pdb:$(PDB)
endif

ifeq ($(ARCH), x32)
    CFLAGS += -Oy-
    LDFLAGS += -safeseh -MACHINE:x86
else
    CFLAGS += -D_WIN64
    LDFLAGS += -MACHINE:x64
endif

# Enables reproducible builds when defined
ifdef SOURCE_DATE_EPOCH
    CFLAGS += -experimental:deterministic
    LDFLAGS += -experimental:deterministic
endif

CPP_SOURCES = \
        jni/com_sun_media_jfxmedia_logging_Logger.cpp   \
        jni/JavaBandsHolder.cpp \
        jni/JavaMediaWarningListener.cpp \
        jni/JavaPlayerEventDispatcher.cpp \
        jni/JniUtils.cpp \
        jni/Logger.cpp \
        jni/NativeVideoBuffer.cpp \
        jni/NativeAudioEqualizer.cpp \
        jni/NativeAudioSpectrum.cpp \
        jni/NativeEqualizerBand.cpp \
        jni/JavaInputStreamCallbacks.cpp \
        PipelineManagement/AudioTrack.cpp \
        PipelineManagement/Pipeline.cpp \
        PipelineManagement/PipelineFactory.cpp \
        PipelineManagement/Track.cpp \
        PipelineManagement/VideoFrame.cpp \
        PipelineManagement/VideoTrack.cpp \
        PipelineManagement/SubtitleTrack.cpp \
        MediaManagement/Media.cpp \
        MediaManagement/MediaManager.cpp \
        Locator/Locator.cpp \
        Locator/LocatorStream.cpp \
        platform/gstreamer/GstMedia.cpp \
        platform/gstreamer/GstMediaPlayer.cpp \
        platform/gstreamer/GstPlatform.cpp \
        platform/gstreamer/GstAudioEqualizer.cpp \
        platform/gstreamer/GstAudioPlaybackPipeline.cpp \
        platform/gstreamer/GstAudioSpectrum.cpp \
        platform/gstreamer/GstAVPlaybackPipeline.cpp \
        platform/gstreamer/GstElementContainer.cpp \
        platform/gstreamer/GstJniUtils.cpp \
        platform/gstreamer/GstMediaManager.cpp \
        platform/gstreamer/GstPipelineFactory.cpp \
        platform/gstreamer/GstVideoFrame.cpp \
        Utils/MediaWarningDispatcher.cpp \
        Utils/LowLevelPerf.cpp \
        Utils/win32/WinCriticalSection.cpp  \
        Utils/win32/WinDllMain.cpp \
        Utils/win32/WinThread.cpp \
        Utils/win32/WinExceptionHandler.cpp

C_SOURCES = Utils/ColorConverter.c

OBJ_DIRS = $(addprefix $(OBJBASE_DIR)/,$(DIRLIST))

DEP_DIRS = $(BUILD_DIR) $(OBJ_DIRS)

DEPTOKEN = "\# makedepend"
DEPFILE = $(OBJBASE_DIR)/Depend.mak

OBJECTS = $(patsubst %.cpp,$(OBJBASE_DIR)/%.obj,$(CPP_SOURCES)) $(patsubst %.c,$(OBJBASE_DIR)/%.obj,$(C_SOURCES))

.PHONY: clean default list

default: $(TARGET)

$(TARGET): $(DEPFILE) $(OBJECTS)
	$(LINKER) $(LDFLAGS) $(shell cygpath -ma "$(RESOURCE)") $(foreach object,$(OBJECTS),$(shell cygpath -ma $(object)))

$(DEPFILE): | $(DEP_DIRS)
	@echo $(DEPTOKEN) > $(DEPFILE)
	makedepend -DTARGET_OS_WIN32=1 $(BASE_INCLUDES) $(shell cygpath -u "$(GENERATED_HEADERS_DIR)") -Y -o.obj -f$(DEPFILE) -s$(DEPTOKEN) $(addprefix $(SRCBASE_DIR)/,$(CPP_SOURCES)) >& /dev/null

$(DEP_DIRS):
	mkdir -p $(DEP_DIRS)

-include $(DEPFILE)

$(OBJBASE_DIR)/%.obj: $(SRCBASE_DIR)/%.cpp
	$(CC) $(CFLAGS) -TP -c -Fo$(shell cygpath -ma $@) $<

$(OBJBASE_DIR)/%.obj: $(SRCBASE_DIR)/%.c
	$(CC) $(CFLAGS) -TC -c -Fo$(shell cygpath -ma $@) $<
