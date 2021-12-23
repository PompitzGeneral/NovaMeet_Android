package com.example.novameet.record

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.novameet.R
import com.example.novameet.databinding.FragmentRecordBinding
import com.example.novameet.network.RetrofitClient


/**
 * 기록 화면
 */
class RecordFragment : Fragment() {
    private val TAG = "RecordFragment"
    private var binding: FragmentRecordBinding? = null;

    var myCookie = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecordBinding.inflate(inflater, container, false)
        val view = binding?.root

        initUI()

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null;
    }

    private fun initUI() {
        var webViewSetting = binding?.webView?.getSettings() //세부 세팅 등록

        webViewSetting?.setJavaScriptEnabled(true) // 웹페이지 자바스크립트 허용 여부
        webViewSetting?.setSupportMultipleWindows(false) // 새창 띄우기 허용 여부
        webViewSetting?.setJavaScriptCanOpenWindowsAutomatically(false) // 자바스크립트 새창 띄우기(멀티뷰) 허용 여부
        webViewSetting?.setLoadWithOverviewMode(true) // 메타태그 허용 여부
        webViewSetting?.setUseWideViewPort(true) // 화면 사이즈 맞추기 허용 여부(true)
        webViewSetting?.setSupportZoom(false) // 화면 줌 허용 여부
        webViewSetting?.setBuiltInZoomControls(false) // 화면 확대 축소 허용 여부
        webViewSetting?.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN) // 컨텐츠 사이즈 맞추기
        webViewSetting?.setCacheMode(WebSettings.LOAD_NO_CACHE) // 브라우저 캐시 허용 여부
        webViewSetting?.setDomStorageEnabled(true) // 로컬저장소 허용 여부
        webViewSetting?.blockNetworkImage = true;
        //webViewSetting?.blockNetworkLoads = true;

        Log.d(TAG, "cookie1: ${CookieManager.getInstance()?.getCookie("https://www.novameet.ga")}")

        binding?.webView?.webViewClient = object : WebViewClient() {
            // 해당 URL이 모두 로딩되었을 때
            override fun onPageFinished(webView: WebView?, url: String) {
                super.onPageFinished(webView, url)
                Log.d(
                    TAG,
                    "cookie2: ${CookieManager.getInstance()?.getCookie("https://www.novameet.ga")}"
                )
                CookieManager.getInstance().flush()
            }
        }

        binding?.webView?.loadUrl("http://www.novameet.ga/#/MobileRecord") // 웹뷰에 표시할 웹사이트 주소, 웹뷰 시작
        Log.d(TAG, "cookie3: ${CookieManager.getInstance()?.getCookie("https://www.novameet.ga")}")
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RecordFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}