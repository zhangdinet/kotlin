/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

enum class IrDeclarationKind {
    MODULE,
    FILE,
    CLASS,
    ENUM_ENTRY,
    FUNCTION,
    CONSTRUCTOR,
    PROPERTY,
    FIELD,
    PROPERTY_ACCESSOR,
    VARIABLE,
    LOCAL_PROPERTY,
    LOCAL_PROPERTY_ACCESSOR,
    TYPEALIAS,
    ANONYMOUS_INITIALIZER,
    TYPE_PARAMETER,
    VALUE_PARAMETER,
    ERROR;
}
