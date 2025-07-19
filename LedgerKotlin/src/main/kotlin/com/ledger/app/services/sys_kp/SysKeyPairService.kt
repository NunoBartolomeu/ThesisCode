package com.ledger.app.services.sys_kp

import java.security.KeyPair

interface SysKeyPairService {
    fun getKeyPair(): KeyPair
}