drop table publicaciones;
drop table descuentos;
drop table compras;
drop table direccion_entrega;
drop table direccion_facturacion;
drop table pagos;
drop table cliente_registrado;
drop table reponer;
drop table proveedor;
drop table referencia;
drop table formato_comercializacion;
drop table producto;

create table producto(
    nombre VARCHAR2(50),
    coffea VARCHAR2(20),
    varietal VARCHAR2(30),
    origen VARCHAR2(15),
    tostado VARCHAR2(10),
    descafeinado VARCHAR2(12),
    CONSTRAINT pk_producto PRIMARY KEY(nombre, coffea, varietal, origen, tostado, descafeinado)
);

create table formato_comercializacion(
    producto_nombre VARCHAR2(50),
    producto_coffea VARCHAR2(20),
    producto_varietal VARCHAR2(30),
    producto_origen VARCHAR2(15),
    producto_tostado VARCHAR2(10),
    producto_descafeinado VARCHAR2(12),
    formato_comercio VARCHAR2(20), 
    empaquetamiento VARCHAR2(15),
    CONSTRAINT pk_formato PRIMARY KEY(producto_nombre, producto_coffea, producto_varietal, producto_origen, producto_tostado, producto_descafeinado, formato_comercio, empaquetamiento),
    CONSTRAINT fk_formato_producto FOREIGN KEY(producto_nombre, producto_coffea, producto_varietal, producto_origen, producto_tostado, producto_descafeinado)
    REFERENCES producto(nombre, coffea, varietal, origen, tostado, descafeinado)
);

create table referencia(
    codigo_barras VARCHAR2(15),
    producto_nombre VARCHAR2(50) NOT NULL,
    producto_coffea VARCHAR2(20) NOT NULL,
    producto_varietal VARCHAR2(30) NOT NULL,
    producto_origen VARCHAR2(15) NOT NULL,
    producto_tostado VARCHAR2(10) NOT NULL,
    producto_descafeinado VARCHAR2(12) NOT NULL,
    PVP VARCHAR2(14) NOT NULL,
    stock NUMBER(5) DEFAULT 0,
    min_stock NUMBER(5) DEFAULT 5,
    max_stock NUMBER(5) NOT NULL,
    CONSTRAINT pk_referencia PRIMARY KEY(codigo_barras),
    CONSTRAINT fk_referencia_producto FOREIGN KEY(producto_nombre, producto_coffea, producto_varietal, producto_origen, producto_tostado, producto_descafeinado)
    REFERENCES producto(nombre, coffea, varietal, origen, tostado, descafeinado)
);

create table proveedor(
    CIF VARCHAR2(10),
    nombre_registrado VARCHAR2(35),
    nombre_persona VARCHAR2(90),
    correo_proveedor VARCHAR2(60),
    n_telefono NUMBER(9),
    cuenta_banco VARCHAR2(30),
    direccion VARCHAR2(120),
    pais VARCHAR2(45),
    CONSTRAINT pk_proveedor PRIMARY KEY(CIF),
    CONSTRAINT sk_proveedor UNIQUE(nombre_registrado, nombre_persona, correo_proveedor, n_telefono, cuenta_banco, direccion, pais)
);

create table reponer(
    referencia_codigo_barras VARCHAR2(15),
    fecha_reponer VARCHAR2(14),
    proveedor_CIF VARCHAR2(10),
    hora VARCHAR2(14),
    coste_producto VARCHAR2(12) NOT NULL, --COST PRICE
    CONSTRAINT pk_reponer PRIMARY KEY(referencia_codigo_barras, fecha_reponer, proveedor_CIF, hora),
    CONSTRAINT fk_reponer_referencia FOREIGN KEY(referencia_codigo_barras) REFERENCES referencia(codigo_barras),
    CONSTRAINT fk_reponer_proveedor FOREIGN KEY(proveedor_CIF) REFERENCES proveedor(CIF)
);

create table cliente_registrado(
    nombre_usuario VARCHAR2(30) NOT NULL,
    contraseña VARCHAR2(15) NOT NULL,
    fecha_registro VARCHAR2(14) NOT NULL,
    hora_registro VARCHAR2(14) NOT NULL,
    nombre_cliente VARCHAR2(35) NOT NULL,
    apellido1_cliente VARCHAR2(30) NOT NULL,
    apellido2_cliente VARCHAR2(30),
    telefono NUMBER(9) UNIQUE,
    correo_usuario VARCHAR2(60) UNIQUE,
    preferencia_contacto VARCHAR(60) DEFAULT 'SMS',
    CONSTRAINT pk_cliente PRIMARY KEY(nombre_usuario)
);

create table pagos(
    num_tarjeta NUMBER(20),
    cliente_nombre_usuario VARCHAR2(30) NOT NULL,
    titular VARCHAR2(30) NOT NULL,
    comp_financiera VARCHAR2(15) NOT NULL,
    fecha_venc VARCHAR2(7) NOT NULL,
    tipo_pago VARCHAR2(15) NOT NULL, --contrarrembolso, tarjeta, transferencia
    fecha_pago VARCHAR2(14) NOT NULL, --payment date
    hora_pago VARCHAR2(14) NOT NULL, --payment hour
    CONSTRAINT pk_pagos PRIMARY KEY(num_tarjeta, fecha_pago, hora_pago),
    CONSTRAINT fk_pagos_cliente FOREIGN KEY(cliente_nombre_usuario) REFERENCES cliente_registrado(nombre_usuario)
);

create table direccion_entrega(
    tipo_via VARCHAR2(10), 
    nombre_via VARCHAR2(30),
    cliente_nombre_usuario VARCHAR2(30),
    num_inmueble VARCHAR2(3),
    num_bloque NUMBER(1),
    escalera VARCHAR2(2),
    piso VARCHAR2(7),
    puerta VARCHAR2(2),
    cod_postal NUMBER(5) NOT NULL,
    ciudad VARCHAR2(45),
    pais VARCHAR2(45), 
    CONSTRAINT pk_direcciones_entrega PRIMARY KEY(tipo_via, nombre_via, num_inmueble, ciudad, pais),
    CONSTRAINT fk_direcciones_entrega_clientes FOREIGN KEY(cliente_nombre_usuario) REFERENCES cliente_registrado(nombre_usuario)
);

create table direccion_facturacion(
    tipo_via VARCHAR2(10), 
    nombre_via VARCHAR2(30),
    cliente_nombre_usuario VARCHAR2(30),
    num_inmueble VARCHAR2(3),
    num_bloque NUMBER(1),
    escalera VARCHAR2(2),
    piso VARCHAR2(7),
    puerta VARCHAR2(2),
    cod_postal NUMBER(5) NOT NULL,
    ciudad VARCHAR2(45),
    pais VARCHAR2(45), 
    CONSTRAINT pk_direcciones_facturacion PRIMARY KEY(tipo_via, nombre_via, num_inmueble, ciudad, pais),
    CONSTRAINT fk_direcciones_facturacion_clientes FOREIGN KEY(cliente_nombre_usuario) REFERENCES cliente_registrado(nombre_usuario)
);

create table compras(
    ID_compra NUMBER GENERATED ALWAYS as IDENTITY(START with 1 INCREMENT by 1),
    cliente_nombre_usuario VARCHAR2(30),
    hora_compra VARCHAR2(14) NOT NULL,
    referencia_codigo_barras VARCHAR2(15) NOT NULL,
    fecha_compra VARCHAR2(14) NOT NULL,
    direccion_entrega_tipo_via VARCHAR2(10) NOT NULL,
    direccion_entrega_nombre_via VARCHAR2(30) NOT NULL,
    direccion_entrega_num_inmueble VARCHAR2(3) NOT NULL,
    direccion_entrega_ciudad VARCHAR2(45) NOT NULL,
    direccion_entrega_pais VARCHAR2(45) NOT NULL,
    nombre_comprador VARCHAR2(35) NOT NULL,
    apellido_comprador VARCHAR2(30) NOT NULL,
    direccion_facturacion_tipo_via VARCHAR2(10) NOT NULL,
    direccion_facturacion_nombre_via VARCHAR2(30) NOT NULL,
    direccion_facturacion_num_inmueble VARCHAR2(3) NOT NULL,
    direccion_facturacion_ciudad VARCHAR2(45) NOT NULL, 
    direccion_facturacion_pais VARCHAR2(45) NOT NULL,
    correo_comprador VARCHAR2(60),
    telefono_comprador NUMBER(9),
    pagos_num_tarjeta NUMBER(20) NOT NULL,
    pagos_fecha_pago VARCHAR2(14) NOT NULL,
    pagos_hora_pago VARCHAR2(14) NOT NULL,
    cantidad NUMBER(2) NOT NULL,
    fecha_entrega VARCHAR2(14) NOT NULL,
    hora_entrega VARCHAR2(14) NOT NULL,
    CONSTRAINT pk_compras PRIMARY KEY(ID_compra),
    CONSTRAINT fk_compras_cliente FOREIGN KEY(cliente_nombre_usuario) REFERENCES cliente_registrado(nombre_usuario),
    CONSTRAINT fk_compras_referencia FOREIGN KEY(referencia_codigo_barras) REFERENCES referencia(codigo_barras),
    CONSTRAINT fk_compras_direccion_entrega FOREIGN KEY(direccion_entrega_tipo_via, direccion_entrega_nombre_via, 
    direccion_entrega_num_inmueble, direccion_entrega_ciudad, direccion_entrega_pais) 
    REFERENCES direccion_entrega(tipo_via, nombre_via, num_inmueble, ciudad, pais),
    CONSTRAINT fk_compras_direccion_facturacion FOREIGN KEY(direccion_facturacion_tipo_via, 
    direccion_facturacion_nombre_via, direccion_facturacion_num_inmueble,
    direccion_facturacion_ciudad, direccion_facturacion_pais) 
    REFERENCES direccion_facturacion(tipo_via, nombre_via, num_inmueble, ciudad, pais),
    CONSTRAINT fk_compras_pagos FOREIGN KEY(pagos_num_tarjeta, pagos_fecha_pago, pagos_hora_pago) REFERENCES pagos(num_tarjeta, fecha_pago, hora_pago)
);

create table descuentos(
    vale VARCHAR2(3) NOT NULL,
    cliente_nombre_usuario VARCHAR2(30),
    CONSTRAINT pk_descuentos PRIMARY KEY(cliente_nombre_usuario),
    CONSTRAINT fk_cliente_nombre_usuario FOREIGN KEY(cliente_nombre_usuario) REFERENCES cliente_registrado(nombre_usuario)
);

create table publicaciones(
    cliente_nombre_usuario VARCHAR2(30),
    puntuacion NUMBER(1) NOT NULL,
    producto_nombre VARCHAR2(50),
    referencia_codigo_barras VARCHAR2(15),
    titulo VARCHAR2(50) NOT NULL,
    texto VARCHAR2(2000) NOT NULL,
    likes NUMBER(9) NOT NULL,
    fecha_publicacion VARCHAR2(14) NOT NULL,
    hora_publicacion VARCHAR2(14) NOT NULL,
    endorsed VARCHAR2(50),
    CONSTRAINT pk_publicaciones PRIMARY KEY(referencia_codigo_barras, cliente_nombre_usuario, fecha_publicacion, hora_publicacion),
    CONSTRAINT fk_publicaciones_referencia FOREIGN KEY(referencia_codigo_barras)
    REFERENCES referencia(codigo_barras),
    CONSTRAINT fk_publicaciones_cliente_registrado FOREIGN KEY(cliente_nombre_usuario) 
    REFERENCES cliente_registrado(nombre_usuario),
    CONSTRAINT ck_publicaciones1 CHECK (0 < puntuacion AND puntuacion < 6),
    CONSTRAINT ck_publicaciones2 CHECK (likes < 1000000000)
);  
