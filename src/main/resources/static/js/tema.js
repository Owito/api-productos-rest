/*
 * Selector de tema: sistema -> claro -> oscuro -> sistema.
 *
 * "Sistema" no fija el atributo data-tema y deja mandar a prefers-color-scheme.
 * La eleccion se guarda en localStorage y la aplica un script sincrono en el
 * <head>, para que la pagina no parpadee al cargar.
 */
(function () {
	'use strict';

	var CICLO = ['sistema', 'claro', 'oscuro'];

	var ETIQUETAS = {
		sistema: { texto: 'Sistema', icono: '◑', descripcion: 'Tema del sistema. Cambiar a claro.' },
		claro: { texto: 'Claro', icono: '☀', descripcion: 'Tema claro. Cambiar a oscuro.' },
		oscuro: { texto: 'Oscuro', icono: '☽', descripcion: 'Tema oscuro. Cambiar al del sistema.' }
	};

	var boton = document.getElementById('selector-tema');
	if (!boton) { return; }

	var etiqueta = document.getElementById('tema-etiqueta');
	var icono = document.getElementById('tema-icono');

	function leer() {
		try {
			var guardado = localStorage.getItem('tema');
			return CICLO.indexOf(guardado) >= 0 ? guardado : 'sistema';
		} catch (e) {
			return 'sistema';
		}
	}

	function guardar(tema) {
		try {
			if (tema === 'sistema') { localStorage.removeItem('tema'); } else { localStorage.setItem('tema', tema); }
		} catch (e) { /* almacenamiento bloqueado: el tema dura solo esta pagina */ }
	}

	function aplicar(tema) {
		if (tema === 'sistema') {
			document.documentElement.removeAttribute('data-tema');
		} else {
			document.documentElement.setAttribute('data-tema', tema);
		}
		var info = ETIQUETAS[tema];
		etiqueta.textContent = info.texto;
		icono.textContent = info.icono;
		boton.setAttribute('aria-label', info.descripcion);
		boton.setAttribute('title', info.descripcion);
	}

	var actual = leer();
	aplicar(actual);

	boton.addEventListener('click', function () {
		actual = CICLO[(CICLO.indexOf(actual) + 1) % CICLO.length];
		guardar(actual);
		aplicar(actual);
	});
})();
