import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IaModulo } from '../../ia-hub';

@Component({
  selector: 'app-ia-panel-activo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ia-panel-activo.html',
  styleUrl: './ia-panel-activo.scss'
}
)
export class IaPanelActivoComponent implements OnInit, OnChanges {
  @Input() modulo!: IaModulo;
  @Input() cargando = false;
  @Input() tieneResultado = false;
  @Input() hitoSeleccionado: 3 | 6 | 12 = 12;
  @Output() ejecutar = new EventEmitter<any>();
  @Output() hitoChange = new EventEmitter<3 | 6 | 12>();

  setHitoInline(meses: 3 | 6 | 12) {
    this.hitoChange.emit(meses);
  }

  // Filtros de fecha
  fechaInicio = '';
  fechaFin = '';
  frecuenciaSelect: 'SEMANAL' | 'QUINCENAL' | 'MENSUAL' = 'MENSUAL';

  // Comprobador de Evolución
  fechaA_inicio = '2026-04-01';
  fechaA_fin = '2026-04-30';
  fechaB_inicio = '2026-05-01';
  fechaB_fin = '2026-05-30';

  // Simulación de metas
  nombreMeta = 'Vacaciones 2026';
  montoObjetivo = 3000;
  montoActualAhorrado = 500;
  aporteMensualDeseado = 200;
  aniosObjetivo = 1;
  mesesAdicionalesObjetivo = 0;

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
    const inicioMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    
    this.fechaInicio = inicioMes.toISOString().split('T')[0];
    this.fechaFin = hoy.toISOString().split('T')[0];
  }

  onEjecutar() {
    let payload: any = {};

    if (this.modulo.id === 'simular-meta') {
      const totalMeses = (this.aniosObjetivo || 0) * 12 + (this.mesesAdicionalesObjetivo || 0);
      payload = {
        nombre_meta: this.nombreMeta,
        monto_objetivo: this.montoObjetivo,
        monto_actual_ahorrado: this.montoActualAhorrado,
        aporte_mensual_deseado: this.aporteMensualDeseado,
        meses_objetivo: totalMeses > 0 ? totalMeses : 12
      };
    } else if (this.modulo.id === 'clasificar-transaccion') {
      payload = {
        id_temporal: this.idTemporal,
        tipo_movimiento: this.tipoMovimiento,
        etiquetas: this.etiquetas,
        descripcion: this.notas,
        notas: this.notas
      };
    } else if (this.modulo.id === 'comprobador-evolucion') {
      payload = {
        rango_a_inicio: this.fechaA_inicio + 'T00:00:00',
        rango_a_fin: this.fechaA_fin + 'T23:59:59',
        rango_b_inicio: this.fechaB_inicio + 'T00:00:00',
        rango_b_fin: this.fechaB_fin + 'T23:59:59'
      };
    } else {
      // Filtros de fecha estándar mapeados al formato PeticionConFiltroFecha del backend
      if (this.modulo.filtroFecha && this.fechaInicio && this.fechaFin) {
        const partsInicio = this.fechaInicio.split('-');
        const partsFin = this.fechaFin.split('-');
        
        payload = {
          anio_inicio: parseInt(partsInicio[0], 10),
          mes_inicio: parseInt(partsInicio[1], 10),
          dia_inicio: parseInt(partsInicio[2], 10),
          anio_fin: parseInt(partsFin[0], 10),
          mes_fin: parseInt(partsFin[1], 10),
          dia_fin: parseInt(partsFin[2], 10),
          frecuencia: this.frecuenciaSelect
        };
      } else {
        payload = {
          frecuencia: this.frecuenciaSelect
        };
      }
    }

    this.ejecutar.emit(payload);
  }
}
