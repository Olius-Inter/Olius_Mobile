package com.olius.app.domain.entity

data class Establishment (
    var id:Int,
    val cnpj: String,
    val description: String,
    val isPev: Boolean,
    val qrToken:String,
    val typeId: String,
    val userId: String,
    val address: String

){
     


}