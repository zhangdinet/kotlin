/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.tests.generator;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolve.calls.KotlinResolutionConfigurationKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.utils.Printer;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.test.InTextDirectivesUtils.*;

public class SimpleTestMethodModel implements TestMethodModel {

    @NotNull
    private final File rootDir;
    @NotNull
    private final File file;
    @NotNull
    private final String doTestMethodName;
    @NotNull
    private final Pattern filenamePattern;
    @NotNull
    private final TargetBackend targetBackend;

    private final boolean skipIgnored;

    public SimpleTestMethodModel(
            @NotNull File rootDir,
            @NotNull File file,
            @NotNull String doTestMethodName,
            @NotNull Pattern filenamePattern,
            @Nullable Boolean checkFilenameStartsLowerCase,
            @NotNull TargetBackend targetBackend,
            boolean skipIgnored
    ) {
        this.rootDir = rootDir;
        this.file = file;
        this.doTestMethodName = doTestMethodName;
        this.filenamePattern = filenamePattern;
        this.targetBackend = targetBackend;
        this.skipIgnored = skipIgnored;

        if (checkFilenameStartsLowerCase != null) {
            char c = file.getName().charAt(0);
            if (checkFilenameStartsLowerCase) {
                assert Character.isLowerCase(c) : "Invalid file name '" + file + "', file name should start with lower-case letter";
            }
            else {
                assert Character.isUpperCase(c) : "Invalid file name '" + file + "', file name should start with upper-case letter";
            }
        }
    }

    @Override
    public void generateBody(@NotNull Printer p) {
        String filePath = KotlinTestUtils.getFilePath(file) + (file.isDirectory() ? "/" : "");
        p.println("String fileName = KotlinTestUtils.navigationMetadata(\"", filePath, "\");");


        boolean ignoredTarget = isIgnoredTarget(targetBackend, file);
        boolean ignoredForNewInference = isIgnoredIfNewInference(file);
        boolean shouldBeMuted = ignoredTarget || ignoredForNewInference;
        if (shouldBeMuted) {
            p.println("try {");
            p.pushIndent();
        }

        p.println(doTestMethodName, "(fileName);");

        if (shouldBeMuted) {
            p.popIndent();
            p.println("}");
            p.println("catch (Throwable ignore) {");
            p.pushIndent();

            if (ignoredForNewInference) {
                p.println("if (!" + KotlinResolutionConfigurationKt.class.getCanonicalName() + ".getUSE_NEW_INFERENCE()) {");
                p.pushIndent();
                p.println("throw ignore;");
                p.popIndent();
                p.println("}");
            }

            p.println("return;");
            p.popIndent();
            p.println("}");

            String directive = ignoredTarget ? "IGNORE_BACKEND" : "IGNORE_IF_NEW_INFERENCE_ENABLED";
            String throwIfPassed = "throw new AssertionError(\"Looks like this test can be unmuted. Remove " + directive + " directive for that.\");";

            if (ignoredForNewInference) {
                p.println("if (" + KotlinResolutionConfigurationKt.class.getCanonicalName() + ".getUSE_NEW_INFERENCE()) {");
                p.pushIndent();
                p.println(throwIfPassed);
                p.popIndent();
                p.println("}");
            }
            else {
                p.println(throwIfPassed);
            }
        }
    }

    @Override
    public String getDataString() {
        String path = FileUtil.getRelativePath(rootDir, file);
        assert path != null;
        return KotlinTestUtils.getFilePath(new File(path));
    }

    @Override
    public boolean shouldBeGenerated() {
        return InTextDirectivesUtils.isCompatibleTarget(targetBackend, file);
    }

    @NotNull
    @Override
    public String getName() {
        Matcher matcher = filenamePattern.matcher(file.getName());
        boolean found = matcher.find();
        assert found : file.getName() + " isn't matched by regex " + filenamePattern.pattern();
        assert matcher.groupCount() >= 1 : filenamePattern.pattern();
        String extractedName = matcher.group(1);
        assert extractedName != null : "extractedName should not be null: "  + filenamePattern.pattern();

        String unescapedName;
        if (rootDir.equals(file.getParentFile())) {
            unescapedName = extractedName;
        }
        else {
            String relativePath = FileUtil.getRelativePath(rootDir, file.getParentFile());
            unescapedName = relativePath + "-" + StringUtil.capitalize(extractedName);
        }

        boolean ignored = isIgnoredTargetWithoutCheck(targetBackend, file) ||
                          skipIgnored && isIgnoredTarget(targetBackend, file);
        return (ignored ? "ignore" : "test") + StringUtil.capitalize(TestGeneratorUtil.escapeForJavaIdentifier(unescapedName));
    }

    @Override
    public void generateSignature(@NotNull Printer p) {
        TestMethodModel.DefaultImpls.generateSignature(this, p);
    }
}
