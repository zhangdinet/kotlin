// INSPECTION_CLASS: org.jetbrains.kotlin.android.inspection.AndroidExtensionsPropertyUsageInspection

package com.myapp

import android.app.Activity
import android.os.Bundle
import kotlinx.android.synthetic.main.layout1.*
import kotlinx.android.synthetic.main.layout2.*
import kotlinx.android.synthetic.main.include_layout1.*
import kotlinx.android.synthetic.main.include_layout2.*

public class MyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.layout1)
    }

    val button1 = login1
    val button2 = <warning descr="Usage of Android Extensions property from unrelated layout layout2.xml">login2</warning>

    val includeButton1 = include_login1
    val includeButton2 = include_login2
}

