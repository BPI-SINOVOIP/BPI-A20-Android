/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.eclipse.adt.internal.lint;

import static com.android.SdkConstants.ATTR_ALLOW_BACKUP;
import static com.android.SdkConstants.ATTR_BASELINE_ALIGNED;
import static com.android.SdkConstants.ATTR_CONTENT_DESCRIPTION;
import static com.android.SdkConstants.ATTR_INPUT_TYPE;
import static com.android.SdkConstants.ATTR_PERMISSION;
import static com.android.SdkConstants.ATTR_TRANSLATABLE;
import static com.android.SdkConstants.VALUE_FALSE;

import com.android.tools.lint.checks.AccessibilityDetector;
import com.android.tools.lint.checks.InefficientWeightDetector;
import com.android.tools.lint.checks.ManifestOrderDetector;
import com.android.tools.lint.checks.SecurityDetector;
import com.android.tools.lint.checks.TextFieldDetector;
import com.android.tools.lint.checks.TranslationDetector;

import org.eclipse.core.resources.IMarker;

/** Shared fix class for various builtin attributes */
final class SetAttributeFix extends SetPropertyFix {
    private SetAttributeFix(String id, IMarker marker) {
        super(id, marker);
    }

    @Override
    protected String getAttribute() {
        if (mId.equals(AccessibilityDetector.ISSUE.getId())) {
            return ATTR_CONTENT_DESCRIPTION;
        } else if (mId.equals(InefficientWeightDetector.BASELINE_WEIGHTS.getId())) {
            return ATTR_BASELINE_ALIGNED;
        } else if (mId.equals(SecurityDetector.EXPORTED_SERVICE.getId())) {
            return ATTR_PERMISSION;
        } else if (mId.equals(TextFieldDetector.ISSUE.getId())) {
            return ATTR_INPUT_TYPE;
        } else if (mId.equals(TranslationDetector.MISSING.getId())) {
            return ATTR_TRANSLATABLE;
        } else if (mId.equals(ManifestOrderDetector.ALLOW_BACKUP.getId())) {
            return ATTR_ALLOW_BACKUP;
        } else {
            assert false : mId;
            return "";
        }
    }

    @Override
    protected boolean isAndroidAttribute() {
        if (mId.equals(TranslationDetector.MISSING.getId())) {
            return false;
        }

        return true;
    }

    @Override
    public String getDisplayString() {
        if (mId.equals(AccessibilityDetector.ISSUE.getId())) {
            return "Add content description attribute";
        } else if (mId.equals(InefficientWeightDetector.BASELINE_WEIGHTS.getId())) {
            return "Set baseline attribute";
        } else if (mId.equals(TextFieldDetector.ISSUE.getId())) {
            return "Set input type";
        } else if (mId.equals(SecurityDetector.EXPORTED_SERVICE.getId())) {
            return "Add permission attribute";
        } else if (mId.equals(TranslationDetector.MISSING.getId())) {
            return "Mark this as a non-translatable resource";
        } else if (mId.equals(ManifestOrderDetector.ALLOW_BACKUP.getId())) {
            return "Set the allowBackup attribute to true or false";
        } else {
            assert false : mId;
            return "";
        }
    }

    @Override
    public String getAdditionalProposalInfo() {
        String help = super.getAdditionalProposalInfo();

        if (mId.equals(TranslationDetector.MISSING.getId())) {
            help = "<b>Adds translatable=\"false\" to this &lt;string&gt;.</b><br><br>" + help;
        }

        return help;
    }

    @Override
    protected boolean invokeCodeCompletion() {
        return mId.equals(SecurityDetector.EXPORTED_SERVICE.getId())
                || mId.equals(TextFieldDetector.ISSUE.getId())
                || mId.equals(ManifestOrderDetector.ALLOW_BACKUP.getId());
    }

    @Override
    public boolean selectValue() {
        if (mId.equals(TranslationDetector.MISSING.getId())) {
            return false;
        } else {
            return super.selectValue();
        }
    }

    @Override
    protected String getProposal() {
        if (mId.equals(InefficientWeightDetector.BASELINE_WEIGHTS.getId())) {
            return VALUE_FALSE;
        } else if (mId.equals(TranslationDetector.MISSING.getId())) {
            return VALUE_FALSE;
        }

        return super.getProposal();
    }
}
