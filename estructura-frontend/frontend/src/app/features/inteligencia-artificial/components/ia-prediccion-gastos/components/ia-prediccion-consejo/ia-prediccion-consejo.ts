import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConsejoEstructuradoPredecir } from '../../../../../../../core/models/ia_coach/ia-base.model';

@Component({
  selector: 'app-ia-prediccion-consejo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-prediccion-consejo.html',
  styleUrl: './ia-prediccion-consejo.scss'
})
export class IaPrediccionConsejoComponent implements OnChanges, OnDestroy {
  @Input() textoCompleto: ConsejoEstructuradoPredecir | string | null = null;
  @Output() hablandoCambio = new EventEmitter<boolean>();

  consejoObj = signal<ConsejoEstructuradoPredecir | null>(null);
  consejoTextoFallback = signal<string>('');
  consejoVisible = signal<boolean>(false);
  estaHablando = signal<boolean>(false);

  private typewriterTimeout: any = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['textoCompleto'] && this.textoCompleto) {
      if (typeof this.textoCompleto === 'object') {
        this.consejoObj.set(this.textoCompleto as ConsejoEstructuradoPredecir);
        this.consejoTextoFallback.set('');
        
        // Simular que habla para la animación visual
        this.estaHablando.set(true);
        this.hablandoCambio.emit(true);
        this.consejoVisible.set(true);
        
        this.typewriterTimeout = setTimeout(() => {
          this.estaHablando.set(false);
          this.hablandoCambio.emit(false);
        }, 2000);
      } else {
        this.consejoObj.set(null);
        this.iniciarTypewriter(this.textoCompleto);
      }
    }
  }

  ngOnDestroy(): void {
    if (this.typewriterTimeout) clearTimeout(this.typewriterTimeout);
  }

  private iniciarTypewriter(textoBruto: string): void {
    const texto = textoBruto.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    this.consejoTextoFallback.set('');
    this.consejoVisible.set(true);
    this.estaHablando.set(true);
    this.hablandoCambio.emit(true);
    let i = 0;
    let currentHTML = '';

    const escribir = () => {
      if (i < texto.length) {
        if (texto[i] === '<') {
          let tag = '';
          while (texto[i] !== '>' && i < texto.length) {
            tag += texto[i];
            i++;
          }
          tag += '>';
          currentHTML += tag;
          i++; 
        } else {
          currentHTML += texto[i];
          i++;
        }
        
        this.consejoTextoFallback.set(currentHTML);
        this.typewriterTimeout = setTimeout(escribir, 15);
      } else {
        this.estaHablando.set(false);
        this.hablandoCambio.emit(false);
      }
    };

    if (this.typewriterTimeout) clearTimeout(this.typewriterTimeout);
    this.typewriterTimeout = setTimeout(escribir, 800);
  }
}
