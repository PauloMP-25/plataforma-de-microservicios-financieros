import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/ia_coach/ia-base.model';

interface Baldosa {
  dia: number;
  completada: boolean;
  actual: boolean;
}

@Component({
  selector: 'app-ia-reto-ahorro',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-reto-ahorro.html',
  styleUrl: './ia-reto-ahorro.scss'
})
export class IaRetoAhorroComponent implements OnInit, OnChanges {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // Estado ficticio para permitir al equipo de Cristina/QA alternar y evaluar todas las pantallas
  estadoFicticio = signal<'NUEVO' | 'ACTIVO' | 'VEREDICTO_VICTORIA' | 'VEREDICTO_DERROTA'>('ACTIVO');

  // Racha de llamas animada para disparar micro-interacciones
  rachaAnimada = signal(false);

  // Animación del briefing de máquina de escribir
  briefingEscrito = signal('');
  private typewriterInterval: any;

  ngOnInit() {
    this.detectarEstadoInicial();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['resultado'] && this.resultado) {
      this.detectarEstadoInicial();
      this.gatillarAnimacionRacha();
    }
  }

  private detectarEstadoInicial() {
    if (!this.resultado) return;
    const backendState = this.resultado.insight?.estado_reto || 'ACTIVO';
    if (backendState === 'NUEVO') {
      this.setEstado('NUEVO');
    } else if (backendState === 'VEREDICTO') {
      const exito = this.resultado.insight?.exito !== false;
      this.setEstado(exito ? 'VEREDICTO_VICTORIA' : 'VEREDICTO_DERROTA');
    } else {
      this.setEstado('ACTIVO');
    }
  }

  setEstado(nuevoEstado: 'NUEVO' | 'ACTIVO' | 'VEREDICTO_VICTORIA' | 'VEREDICTO_DERROTA') {
    this.estadoFicticio.set(nuevoEstado);
    if (nuevoEstado === 'NUEVO') {
      this.iniciarBriefingMaquinaEscribir();
    } else {
      if (this.typewriterInterval) clearInterval(this.typewriterInterval);
    }
  }

  // Genera baldosas del mapa (7 días del reto semanal)
  baldosas = computed<Baldosa[]>(() => {
    const progreso = this.resultado?.insight?.progreso_temporal || 45; // porcentaje
    const diasTotales = 7;
    const diaActual = Math.max(1, Math.min(diasTotales, Math.ceil((progreso / 100) * diasTotales)));
    
    return Array.from({ length: diasTotales }, (_, i) => {
      const dia = i + 1;
      return {
        dia,
        completada: dia < diaActual,
        actual: dia === diaActual
      };
    });
  });

  // Gatilla micro-animación de las llamas
  private gatillarAnimacionRacha() {
    this.rachaAnimada.set(true);
    setTimeout(() => this.rachaAnimada.set(false), 800);
  }

  // Animación de máquina de escribir para briefing del cuartel general
  private iniciarBriefingMaquinaEscribir() {
    const textoCompleto = this.resultado?.consejo || 
      '¡Misión: Operación Cocina en Casa! 🏆 Paulo, he detectado que tu "Enemigo Final" de esta semana son los Restaurantes. Tu misión, si decides aceptarla, es evitar comer fuera por los próximos 7 días. Si lo logras, habrás salvado S/ 85.00 para tu fondo de la "Laptop Gamer". ¿Aceptas el reto, Jugador 1?';
    
    this.briefingEscrito.set('');
    if (this.typewriterInterval) clearInterval(this.typewriterInterval);

    let index = 0;
    this.typewriterInterval = setInterval(() => {
      if (index < textoCompleto.length) {
        this.briefingEscrito.update(prev => prev + textoCompleto.charAt(index));
        index++;
      } else {
        clearInterval(this.typewriterInterval);
      }
    }, 15); // Rápido y fluido
  }
}
