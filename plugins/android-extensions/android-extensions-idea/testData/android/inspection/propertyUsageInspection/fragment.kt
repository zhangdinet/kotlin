// INSPECTION_CLASS: org.jetbrains.kotlin.android.inspection.AndroidExtensionsPropertyUsageInspection

package com.myapp

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.layout1.*
import kotlinx.android.synthetic.main.layout2.*
import kotlinx.android.synthetic.main.layout2.view.*
import kotlinx.android.synthetic.main.include_layout1.*
import kotlinx.android.synthetic.main.include_layout2.*

public class MyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val inflatedView = inflater.inflate(R.layout.layout1, container)

        FrameLayout(context).login2
        inflatedView.login2

        <warning descr="Usage of Android Extensions property from unrelated layout layout2.xml">login2</warning>.text = "lorem ipsum"

        return inflatedView
    }

    val button1 = login1
    val button2 = <warning descr="Usage of Android Extensions property from unrelated layout layout2.xml">login2</warning>

    val includeButton1 = include_login1
    val includeButton2 = include_login2
}

