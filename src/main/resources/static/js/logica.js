let prendas = [];
let recursos = [];

// --- GESTIÓN DE PRENDAS ---
function addPrenda() {
    const nombreInput = document.getElementById('nombre-prenda');
    const utilidadInput = document.getElementById('precio-prenda');
    
    const nombre = nombreInput.value.trim();
    const utilidad = parseFloat(utilidadInput.value);

    if (nombre !== "" && !isNaN(utilidad)) {
        prendas.push({ nombre, utilidad });
        
        // Limpiar campos
        nombreInput.value = '';
        utilidadInput.value = '';
        
        // Forzar actualización de toda la interfaz
        actualizarUI();
    } else {
        alert("Por favor, ingresa un nombre y un valor de utilidad válido.");
    }
}

// --- ACTUALIZACIÓN INTEGRAL ---
function actualizarUI() {
    renderListaConfig();    // Sidebar: Lista con botones X
    renderResumen();       // Centro: Los cuadritos de resumen
    renderMatriz();        // Abajo: La matriz de coeficientes (Plan Maestro)
}

function renderMatriz() {
    const container = document.getElementById('matriz-container');
    
    if (prendas.length === 0 || recursos.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:40px; color:#94a3b8;">
                <i class="fas fa-th-list" style="font-size:2rem; display:block; margin-bottom:10px;"></i>
                Añade prendas y recursos en el panel izquierdo para generar la matriz de consumo.
            </div>`;
        return;
    }

    let html = `
        <table class="matrix-table">
            <thead>
                <tr>
                    <th>Recurso</th>
                    ${prendas.map(p => `<th>${p.nombre}</th>`).join('')}
                    <th></th> <!-- Columna para el operador -->
                    <th style="background:#eef2ff">Límite (Disponibilidad)</th>
                </tr>
            </thead>
            <tbody>
                ${recursos.map((r, i) => `
                    <tr>
                        <td><strong>${r}</strong></td>
                        ${prendas.map((_, j) => `<td><input type="number" class="val-cell" value="0"></td>`).join('')}
                        <td style="width: 60px;">
                            <select class="operator-select" style="padding: 4px; border-radius: 4px; border: 1px solid #cbd5e1;">
                                <option value="<=">&le;</option>
                                <option value=">=">&ge;</option>
                                <option value="=">=</option>
                            </select>
                        </td>
                        <td style="background:#f8fafc">
                            <input type="number" class="val-cell" style="font-weight:bold; border-color:var(--accent)" placeholder="Total">
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    container.innerHTML = html;
}

function removePrenda(index) {
    prendas.splice(index, 1);
    actualizarUI();
}

// --- GESTIÓN DE RECURSOS ---
function addRecurso() {
    const nombre = document.getElementById('nombre-recurso').value;
    if (nombre) {
        recursos.push(nombre);
        document.getElementById('nombre-recurso').value = '';
        actualizarUI();
    }
}

function removeRecurso(index) {
    recursos.splice(index, 1);
    actualizarUI();
}

// --- RENDERIZADO GENERAL ---

function renderListaConfig() {
    // Lista de prendas
    const pList = document.getElementById('productos-list');
    pList.innerHTML = prendas.map((p, i) => `
        <div class="product-card">
            <span>${p.nombre} ($${p.utilidad})</span>
            <button onclick="removePrenda(${i})" class="btn-delete"><i class="fas fa-times"></i></button>
        </div>
    `).join('');

    // Lista de recursos corregida (sin el selector aquí)
    const rList = document.getElementById('recursos-list-items');
    rList.innerHTML = recursos.map((r, i) => `
        <div class="product-card">
            <span>${r}</span>
            <button onclick="removeRecurso(${i})" class="btn-delete"><i class="fas fa-times"></i></button>
        </div>
    `).join('');
}

function renderResumen() {
    const visor = document.getElementById('resumen-lista-simple');
    const contador = document.getElementById('count-prendas');
    
    contador.innerText = `${prendas.length} Prendas`;
    visor.innerHTML = prendas.map(p => `
        <div class="summary-item">
            <small>${p.nombre}</small>
            <span class="profit">$${p.utilidad.toFixed(2)}</span>
        </div>
    `).join('');
}


async function optimizar() {
    const utilidades = prendas.map(p => p.utilidad);
    const nombres = prendas.map(p => p.nombre);
    const nombresRecursos = recursos; 

    // Los operadores ahora se capturan directamente de las filas de la tabla
    const listaDeOperadores = Array.from(document.querySelectorAll('.operator-select')).map(select => select.value);

    const filasMatriz = [];
    const filasTabla = document.querySelectorAll(".matrix-table tbody tr");

    filasTabla.forEach(fila => {
        const valoresFila = [];
        // Seleccionamos solo los inputs de tipo número (coeficientes + disponibilidad)
        const inputs = fila.querySelectorAll("input[type='number']");
        inputs.forEach(input => {
            valoresFila.push(parseFloat(input.value) || 0);
        });
        filasMatriz.push(valoresFila);
    });

    // ... resto del código fetch igual que antes ...
    const datosParaEnviar = {
        matriz: filasMatriz,
        utilidades: utilidades,
        operadores: listaDeOperadores,
        nombres: nombres,
        nombresRecursos: nombresRecursos
    };

    // ... try/catch del fetch ...
    try {
        const urlBackend = 'http://127.0.0.1:8080/api/simplex/optimizar';
        const response = await fetch(urlBackend, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datosParaEnviar)
        });

        if (!response.ok) throw new Error("Error en la respuesta");

        const resultado = await response.json();
        
        // 1. Primero dibujamos los resultados
        mostrarResultados(resultado);

        // 2. LUEGO hacemos el scroll suave hacia el área de resultados
        const areaResultados = document.getElementById('results-area');
        areaResultados.scrollIntoView({ 
            behavior: 'smooth', // Hace que el movimiento sea fluido y no un salto brusco
            block: 'start'      // Alinea el inicio del área de resultados con la parte superior
        });

    } catch (error) {
        console.error("Error:", error);
        alert("Error de conexión.");
    }
}


function mostrarResultados(res) {
    const area = document.getElementById('results-area');
    area.classList.remove('hidden');
    
    if (!res.esFactible) {
        area.innerHTML = `<div class="card" style="border-left: 5px solid red">
            <h3><i class="fas fa-exclamation-triangle"></i> Error</h3>
            <p>${res.mensaje}</p>
        </div>`;
        return;
    }

    // Mostramos el Plan Maestro
    area.innerHTML = `
        <div class="card" style="border-left: 5px solid green; margin-bottom: 20px;">
            <h3><i class="fas fa-check-circle"></i> Plan Maestro Generado</h3>
            <p><strong>Utilidad Máxima:</strong> $${res.utilidadOptima.toFixed(2)}</p>
            <p>${res.mensaje}</p>
        </div>
        <div id="seccion-sensibilidad" class="card"></div> 
    `;
    
    // Llamamos a la función que dibuja la tabla de sensibilidad
    mostrarSensibilidad(res);
}

function mostrarSensibilidad(res) {
    const contenedor = document.getElementById('seccion-sensibilidad');
    if (!res.analisisRecursos || res.analisisRecursos.length === 0) return;

    let html = `
        <h3><i class="fas fa-chart-line"></i> Análisis de Sensibilidad de Recursos</h3>
        <table class="results-table">
            <thead>
                <tr>
                    <th style="width: 25%">Recurso</th>
                    <th style="width: 15%">Precio Sombra</th>
                    <th style="width: 20%">Estado</th>
                    <th style="width: 40%">Rango de Estabilidad (Mín - Máx)</th>
                </tr>
            </thead>
            <tbody>`;

    res.analisisRecursos.forEach(recurso => {
        const precio = recurso.precioSombra.toFixed(2);
        const estado = recurso.precioSombra > 0 ? 
            '<span class="badge escaso"><i class="fas fa-lock"></i> Escaso</span>' : 
            '<span class="badge abundante"><i class="fas fa-check"></i> Abundante</span>';
        
        const limInf = recurso.limiteInferior.toFixed(2);
        const limSup = (recurso.limiteSuperior === null || recurso.limiteSuperior > 1e15) ? 
            "∞" : recurso.limiteSuperior.toFixed(2);

        html += `
            <tr>
                <td><strong>${recurso.nombre}</strong></td>
                <td style="color: ${recurso.precioSombra > 0 ? '#059669' : '#64748b'}; font-weight: bold;">
                    $${precio}
                </td>
                <td>${estado}</td>
                <td>
                    <span class="rango-text">[ ${limInf} &nbsp;&mdash;&nbsp; ${limSup} ]</span>
                </td>
            </tr>`;
    });

    html += `</tbody></table>`;
    contenedor.innerHTML = html;
}

// Iniciar vacio
actualizarUI();