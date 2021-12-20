package com.dabbleam.dgc.validator

import android.content.Context
import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import ehn.techiop.hcert.kotlin.crypto.CertificateAdapter
import ehn.techiop.hcert.kotlin.trust.TrustedCertificateV2
import java.io.File
import java.io.FileInputStream

data class JSONTrustListEntry(
    @Json(name = "clavePublica")
    val publicKey: String,
    @Json(name = "tipo")
    val type: String,           // Seems to always be DSC for now
    @Json(name = "certificado")
    val certificate: String,
    @Json(name = "kid")
    val kid: String
)

class TrustList( context: Context )
{
    val ctx = context

    fun loadTrustList(): List<JSONTrustListEntry>?
    {
        val cachedList = File( ctx.filesDir, "trustList.json" )
        var trustListJSON = ""
        if ( cachedList.exists() )
        {
            trustListJSON = FileInputStream( cachedList ).bufferedReader().use { it.readText() }
        }
        if ( trustListJSON == "" )
        {
            // didn't find cached trust list, load base list included with app package
            trustListJSON = ctx.resources.openRawResource( R.raw.base_trust_list ).bufferedReader().use { it.readText() }
        }
        val trustList = Klaxon().parseArray<JSONTrustListEntry>( trustListJSON )
        return trustList
    }

    fun getTrustList( payload: List<JSONTrustListEntry> ): List<TrustedCertificateV2>
    {
        val list = payload.map { CertificateAdapter( it.certificate ).toTrustedCertificate() }
        return list
    }
}