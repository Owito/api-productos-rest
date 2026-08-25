-- Dos productos no pueden llamarse igual.
--
-- La regla ya la comprueba el caso de uso antes de guardar, y esa comprobacion
-- se queda: es la que permite responder un 409 con un mensaje legible. Pero es
-- leer-y-despues-escribir, y entre las dos operaciones cabe otra peticion. Esta
-- restriccion es la que no se puede burlar por concurrencia.
--
-- Va en una version aparte de V1 justamente para que se aplique en la base de
-- produccion, que se adopto con baseline y por tanto nunca ejecuta V1.
--
-- Cubre la coincidencia exacta. La regla completa ignora mayusculas y esa sigue
-- viviendo en el caso de uso: expresarla aqui pediria un indice funcional sobre
-- lower(nombre), que PostgreSQL soporta y H2 no. Ponerlo en un solo motor haria
-- que local y produccion se comportaran distinto, que es peor que la limitacion.

alter table productos add constraint uk_productos_nombre unique (nombre);
