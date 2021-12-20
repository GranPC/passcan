package com.dabbleam.dgc.validator

import android.util.Log
import ehn.techiop.hcert.kotlin.chain.CertificateRepository
import ehn.techiop.hcert.kotlin.chain.Error
import ehn.techiop.hcert.kotlin.chain.VerificationException
import ehn.techiop.hcert.kotlin.chain.VerificationResult
import ehn.techiop.hcert.kotlin.crypto.CertificateAdapter
import ehn.techiop.hcert.kotlin.trust.TrustedCertificateV2

// as expected, the official app doesn't use any of the proper certificate repository handling
// in fact they manually patched the code in TrustListCertificateRepository...

// we can do a bit better.

class ManualCertificateRepository(certificates: List<TrustedCertificateV2>) : CertificateRepository
{
    private val list = certificates

    override fun loadTrustedCertificates(
        kid: ByteArray,
        verificationResult: VerificationResult
    ): List<CertificateAdapter> {
        val certList = list.filter { it.kid contentEquals kid }
        if (certList.isEmpty())
            throw VerificationException(Error.KEY_NOT_IN_TRUST_LIST, "kid not found")

        return certList.map {
            it.toCertificateAdapter()
        }
    }

}
