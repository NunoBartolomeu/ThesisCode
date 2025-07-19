package com.ledger.app.models.ledger

data class Node(
    val hash: ByteArray,
    val height: Int,
    val index: Int,
    val left: Node?,
    val right: Node?
)