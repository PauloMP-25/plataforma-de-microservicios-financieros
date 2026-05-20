import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IaModulo } from '../../../../layout/sidebar/sidebar/ia-panel/ia-panel';

@Component({
  selector: 'app-ia-panel-activo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ia-panel-activo.html',
  styleUrl: './ia-panel-activo.scss'
})
export class IaPanelActivoComponent implements OnInit, OnChanges {
  @Input() modulo!: IaModulo;
  @Input() cargando = false;
  @Output() ejecutar = new EventEmitter<any>();

  // Filtros de fecha (predeterminados al mes actual)
  mesSelect = 5; // Mayo
  anioSelect = 2026;
  tamanioPagina = 100;
  frecuenciaSelect: 'SEMANAL' | 'QUINCENAL' | 'MENSUAL' = 'MENSUAL';

  // Simulación de metas
  nombreMeta = 'Vacaciones 2026';
  montoObjetivo = 3000;
  montoActualAhorrado = 500;
  aporteMensualDeseado = 200;

  // Clasificación de transacciones
  idTemporal = 'tx_temp_001';
  tipoMovimiento: 'INGRESO' | 'GASTO' = 'GASTO';
  etiquetas = 'comida, delivery, fin de semana';
  notas = 'Pedido de hamburguesa familiar';

  ngOnInit() {
    this.resetFormularios();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['modulo']) {
      this.resetFormularios();
    }
  }

  resetFormularios() {
    const hoy = new Date();
    this.mesSelect = hoy.getMonth() + 1;
    this.anioSelect = hoy.getFullYear();
  }

  onEjecutar() {
    let payload: any = {};

    if (this.modulo.id === 'simular-meta') {
      payload = {
        nombre_meta: this.nombreMeta,
        monto_objetivo: this.montoObjetivo,
        monto_actual_ahorrado: this.montoActualAhorrado,
        aporte_mensual_deseado: this.aporteMensualDeseado
      };
    } else if (this.modulo.id === 'clasificar-transaccion') {
      payload = {
        id_temporal: this.idTemporal,
        tipo_movimiento: this.tipoMovimiento,
        etiquetas: this.etiquetas,
        notas: this.notas
      };
    } else {
      // Filtros de fecha estándar
      payload = {
        mes: Number(this.mesSelect),
        anio: Number(this.anioSelect),
        tamanio_pagina: Number(this.tamanioPagina),
        frecuencia: this.frecuenciaSelect
      };
    }

    this.ejecutar.emit(payload);
  }
}
