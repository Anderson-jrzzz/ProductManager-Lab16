package com.tecsup.productmanager.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.productmanager.models.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var productos by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var productoEditado by remember { mutableStateOf<Producto?>(null) }

    var nombre by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }

    // Agregar estado para mensajes de error
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Iniciando listener para userId: $userId")

        firestore.collection("productos")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeScreen", "Error al obtener productos: ${error.message}")
                    errorMessage = "Error al cargar productos: ${error.message}"
                    showError = true
                    return@addSnapshotListener
                }

                Log.d("HomeScreen", "Snapshot recibido. Documentos: ${snapshot?.documents?.size ?: 0}")

                snapshot?.documents?.forEach { doc ->
                    Log.d("HomeScreen", "Documento ${doc.id}: ${doc.data}")
                }

                productos = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        // Obtener datos del documento
                        val nombreDoc = doc.getString("nombre") ?: ""
                        val precioStr = doc.getString("precio") ?: "0.0"
                        val categoriaDoc = doc.getString("categoria") ?: ""
                        val userIdDoc = doc.getString("userId") ?: ""

                        // Convertir precio a Double
                        val precioDouble = precioStr.toDoubleOrNull() ?: 0.0

                        // Obtener stock (puede venir como Long o String)
                        val stockInt = when {
                            doc.get("stock") is Long -> (doc.get("stock") as Long).toInt()
                            doc.get("stock") is String -> (doc.get("stock") as String).toIntOrNull() ?: 0
                            else -> 0
                        }

                        Producto(
                            id = doc.id,
                            nombre = nombreDoc,
                            precio = precioDouble,
                            stock = stockInt,
                            categoria = categoriaDoc,
                            userId = userIdDoc
                        )
                    } catch (e: Exception) {
                        Log.e("HomeScreen", "Error al mapear documento ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                Log.d("HomeScreen", "Productos mapeados: ${productos.size}")
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Mis Productos",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // Mostrar email del usuario
                    Text(
                        text = auth.currentUser?.email?.takeWhile { it != '@' } ?: "Usuario",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = {
                        auth.signOut()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    productoEditado = null
                    nombre = ""
                    precio = ""
                    stock = ""
                    categoria = ""
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (productos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Sin productos",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay productos registrados",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Presiona el botón + para agregar tu primer producto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(productos) { producto ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = producto.nombre,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                productoEditado = producto
                                                nombre = producto.nombre
                                                precio = String.format("%.2f", producto.precio)
                                                stock = producto.stock.toString()
                                                categoria = producto.categoria
                                                showDialog = true
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar producto",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                firestore.collection("productos")
                                                    .document(producto.id)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        Log.d("HomeScreen", "Producto eliminado: ${producto.id}")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("HomeScreen", "Error al eliminar: ${e.message}")
                                                        errorMessage = "Error al eliminar producto"
                                                        showError = true
                                                    }
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar producto",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Detalles del producto
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Precio:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "S/. ${String.format("%.2f", producto.precio)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Stock disponible:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${producto.stock} unidades",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (producto.stock > 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Categoría:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = producto.categoria,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogo para agregar/editar producto
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (productoEditado == null) "Nuevo Producto" else "Editar Producto",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del producto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nombre.isBlank(),
                        supportingText = {
                            if (nombre.isBlank()) {
                                Text("Este campo es requerido")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = precio,
                        onValueChange = {
                            // Permitir solo números y un punto decimal
                            val filtered = it.filter { char ->
                                char.isDigit() || char == '.' || char == ','
                            }.replace(',', '.')

                            // Validar que solo haya un punto decimal
                            if (filtered.count { c -> c == '.' } <= 1) {
                                precio = filtered
                            }
                        },
                        label = { Text("Precio (S/.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = precio.isBlank() || precio.toDoubleOrNull() == null,
                        supportingText = {
                            if (precio.isBlank()) {
                                Text("Este campo es requerido")
                            } else if (precio.toDoubleOrNull() == null) {
                                Text("Ingrese un precio válido")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = stock,
                        onValueChange = {
                            // Permitir solo números
                            val filtered = it.filter { char -> char.isDigit() }
                            stock = filtered
                        },
                        label = { Text("Stock disponible") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stock.isBlank() || stock.toIntOrNull() == null,
                        supportingText = {
                            if (stock.isBlank()) {
                                Text("Este campo es requerido")
                            } else if (stock.toIntOrNull() == null) {
                                Text("Ingrese un número válido")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = categoria,
                        onValueChange = { categoria = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = categoria.isBlank(),
                        supportingText = {
                            if (categoria.isBlank()) {
                                Text("Este campo es requerido")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Validar campos
                        if (nombre.isBlank() || precio.isBlank() || stock.isBlank() || categoria.isBlank()) {
                            errorMessage = "Complete todos los campos requeridos"
                            showError = true
                            return@Button
                        }

                        val precioDouble = precio.toDoubleOrNull()
                        val stockInt = stock.toIntOrNull()

                        if (precioDouble == null) {
                            errorMessage = "Ingrese un precio válido"
                            showError = true
                            return@Button
                        }

                        if (stockInt == null) {
                            errorMessage = "Ingrese un stock válido"
                            showError = true
                            return@Button
                        }

                        val productoData = hashMapOf(
                            "nombre" to nombre.trim(),
                            "precio" to precioDouble.toString(),
                            "stock" to stockInt,
                            "categoria" to categoria.trim(),
                            "userId" to userId
                        )

                        if (productoEditado == null) {
                            // Agregar nuevo producto
                            firestore.collection("productos")
                                .add(productoData)
                                .addOnSuccessListener {
                                    Log.d("HomeScreen", "Producto agregado con ID: ${it.id}")
                                    showDialog = false
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HomeScreen", "Error al agregar producto: ${e.message}")
                                    errorMessage = "Error al guardar producto: ${e.message}"
                                    showError = true
                                }
                        } else {
                            // Actualizar producto existente
                            firestore.collection("productos")
                                .document(productoEditado!!.id)
                                .set(productoData)
                                .addOnSuccessListener {
                                    Log.d("HomeScreen", "Producto actualizado: ${productoEditado!!.id}")
                                    showDialog = false
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HomeScreen", "Error al actualizar producto: ${e.message}")
                                    errorMessage = "Error al actualizar producto: ${e.message}"
                                    showError = true
                                }
                        }
                    },
                    enabled = nombre.isNotBlank() && precio.isNotBlank() &&
                            stock.isNotBlank() && categoria.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Snackbar para errores
    if (showError && errorMessage != null) {
        LaunchedEffect(showError) {
            // Auto cerrar después de 3 segundos
            kotlinx.coroutines.delay(3000)
            showError = false
            errorMessage = null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Text(text = errorMessage ?: "Ocurrió un error")
            }
        }
    }
}