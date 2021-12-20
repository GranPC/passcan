package com.dabbleam.dgc.validator

import ehn.techiop.hcert.kotlin.chain.CertificateRepository
import ehn.techiop.hcert.kotlin.chain.Chain
import ehn.techiop.hcert.kotlin.chain.impl.*
import kotlinx.datetime.Clock
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.time.ExperimentalTime

// it looks like Spanish certificates have expirationTime > certValidUntil
// this seems to either be a validation of the spec, or way too strict checking
// in the EHN library. replace CWT service with one that doesn't care.

object LooseChain {
    /**
     * Builds a "default" chain for verifying, i.e. one with the implementation according to spec.
     */
    @ExperimentalTime
    @JvmStatic
    @JvmOverloads
    fun buildVerificationChain(repository: CertificateRepository, clock: Clock = Clock.System) = Chain(
        DefaultHigherOrderValidationService(),
        DefaultSchemaValidationService(),
        DefaultCborService(),
        LooseCwtService(clock = clock),
        DefaultCoseService(repository),
        DefaultCompressorService(),
        DefaultBase45Service(),
        DefaultContextIdentifierService()
    )
}