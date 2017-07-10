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
        setContentViewToLayout1()
    }

    val button1 = login1
    val button2 = login2 // No error since no layout references found in MyActivity

    val includeButton1 = include_login1
    val includeButton2 = include_login2
}

fun Activity.setContentViewToLayout1() {
    setContentView(R.layout.layout1)
}