package com.healthmetrix.dynamicconsent.commons.pdf

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import java.security.Security

@Component
class BouncyCastleInitializer : InitializingBean {
    override fun afterPropertiesSet() {
        Security.addProvider(BouncyCastleProvider())
    }
}
