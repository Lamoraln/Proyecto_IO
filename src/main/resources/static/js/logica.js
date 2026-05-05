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
    
    // Si no hay nada, mostramos mensaje amigable
    if (prendas.length === 0 || recursos.length === 0) {
        container.innerHTML = `
            <div style="text-align:center; padding:40px; color:#94a3b8;">
                <i class="fas fa-th-list" style="font-size:2rem; display:block; margin-bottom:10px;"></i>
                Añade prendas y recursos en el panel izquierdo para generar la matriz de consumo.
            </div>`;
        return;
    }

    // Si hay datos, construimos la tabla
    let html = `
        <table class="matrix-table">
            <thead>
                <tr>
                    <th>Recurso</th>
                    ${prendas.map(p => `<th>${p.nombre}</th>`).join('')}
                    <th style="background:#eef2ff">Límite (Disponibilidad)</th>
                </tr>
            </thead>
            <tbody>
                ${recursos.map((r, i) => `
                    <tr>
                        <td><strong>${r}</strong></td>
                        ${prendas.map((_, j) => `<td><input type="number" class="val-cell" value="0"></td>`).join('')}
                        <td style="background:#f8fafc"><input type="number" class="val-cell" style="font-weight:bold; border-color:var(--accent)" placeholder="Total"></td>
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
    // Lista de prendas en sidebar
    const pList = document.getElementById('productos-list');
    pList.innerHTML = prendas.map((p, i) => `
        <div class="product-card">
            <span>${p.nombre} ($${p.utilidad})</span>
            <button onclick="removePrenda(${i})" class="btn-delete"><i class="fas fa-times"></i></button>
        </div>
    `).join('');

    // Lista de recursos en sidebar
    const rList = document.getElementById('recursos-list-items');
    rList.innerHTML = recursos.map((r, i) => `
        <div class="product-card">
            <span>${r}</span>
            <button onclick="removeRecurso(${i})" class="btn-delete"><i class="fas fa-times"></i></button>
            <td>
                <select class="operator-select">
                    <option value="<=">&le;</option>
                    <option value=">=">&ge;</option>
                    <option value="=">=</option>
                </select>
            </td>
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
    // 1. Extraer las utilidades (ganancias) de las prendas
    const utilidades = prendas.map(p => p.utilidad);
    const nombres = prendas.map(p => p.nombre);
    const listaDeOperadores = Array.from(document.querySelectorAll('.operator-select')).map(select => select.value);

    // 2. Extraer la matriz de la tabla dinámica
    const filasMatriz = [];
    const filasTabla = document.querySelectorAll(".matrix-table tbody tr");

    filasTabla.forEach(fila => {
        const valoresFila = [];
        const inputs = fila.querySelectorAll("input");
        
        inputs.forEach(input => {
            valoresFila.push(parseFloat(input.value) || 0);
        });
        
        filasMatriz.push(valoresFila);
    });

    // 3. Preparar el objeto para enviar
    const datosParaEnviar = {
        matriz: filasMatriz,
        utilidades: utilidades,
        operadores: listaDeOperadores,
        nombres: nombres
    };

    try {
        const urlBackend = 'http://127.0.0.1:8080/api/simplex/optimizar?v=' + Math.random();
        console.log("Intentando conectar a:", urlBackend);

        const response = await fetch(urlBackend, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(datosParaEnviar)
        });

        const resultado = await response.json();
        mostrarResultados(resultado);

    } catch (error) {
        console.error("Error detallado:", error);
        alert("MENSAJE DE PRUEBA: Si ves esto, el JS es el nuevo");
    }

}

function mostrarResultados(res) {
    const area = document.getElementById('results-area');
    area.classList.remove('hidden');
    
    if (!res.esFactible) {
        area.innerHTML = `<div class="card" style="border-left: 5px solid var(--danger)">
            <h3><i class="fas fa-exclamation-triangle"></i> Error en el Plan</h3>
            <p>${res.mensaje}</p>
        </div>`;
        return;
    }

    area.innerHTML = `
        <div class="card" style="border-left: 5px solid var(--success)">
            <h3><i class="fas fa-check-circle"></i> Plan Maestro Generado</h3>
            <p><strong>Utilidad Máxima Estimada:</strong> $${res.utilidadOptima.toFixed(2)}</p>
            <p class="helper">${res.mensaje}</p>
            <hr>
            <p><small>Próximo paso: Procesar Análisis de Sensibilidad...</small></p>
        </div>
    `;
    
    // Hacemos scroll suave hasta los resultados
    area.scrollIntoView({ behavior: 'smooth' });
}

// Iniciar vacio
actualizarUI();