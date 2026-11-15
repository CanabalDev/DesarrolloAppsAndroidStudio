package com.example.desarrooapps;

final class Finca {
    final String id;
    final String nombreFinca;
    final double areaHa;
    final double areaM2;
    final Usuario propietario;
    final Usuario capataz;
    final String pais;
    final String departamento;
    final String ciudad;
    final boolean produceLeche;
    final boolean produceCereales;
    final boolean produceFrutas;
    final boolean produceVerduras;

    Finca(
            String id,
            String nombreFinca,
            double areaHa,
            double areaM2,
            Usuario propietario,
            Usuario capataz,
            String pais,
            String departamento,
            String ciudad,
            boolean produceLeche,
            boolean produceCereales,
            boolean produceFrutas,
            boolean produceVerduras
    ) {
        this.id = id;
        this.nombreFinca = nombreFinca;
        this.areaHa = areaHa;
        this.areaM2 = areaM2;
        this.propietario = propietario;
        this.capataz = capataz;
        this.pais = pais;
        this.departamento = departamento;
        this.ciudad = ciudad;
        this.produceLeche = produceLeche;
        this.produceCereales = produceCereales;
        this.produceFrutas = produceFrutas;
        this.produceVerduras = produceVerduras;
    }
}
