package com.tecsup.productmanager.models

//Para productos: nombre, precio, stock, categoría
data class Producto (
    val id: String="",
    val nombre: String = "",
    val precio: Double = 0.0,
    val stock: Int = 0,
    val categoria: String = "",
    val userId: String = ""
)