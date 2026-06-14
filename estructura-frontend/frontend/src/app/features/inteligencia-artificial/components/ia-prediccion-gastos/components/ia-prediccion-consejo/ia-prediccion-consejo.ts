import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-prediccion-consejo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-prediccion-consejo.html',
  styleUrl: './ia-prediccion-consejo.scss'
})
export class IaPrediccionConsejoComponent implements OnChanges, OnDestroy {
  @Input() textoCompleto: any = null;
  @Output() hablandoCambio = new EventEmitter<boolean>();

  consejoTexto = signal<string>('');
  consejoVisible = signal<boolean>(false);
  estaHablando = signal<boolean>(false);

  private typewriterTimeout: any = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['textoCompleto'] && this.textoCompleto) {
      let textoConstruido = '';
      if (typeof this.textoCompleto === 'object') {
        textoConstruido = `
          <p>${this.textoCompleto.introduccion}</p>
          <p>${this.textoCompleto.analisis_tendencia}</p>
          <p><strong>Impacto:</strong> ${this.textoCompleto.impacto_meta}</p>
          <p class="highlight-math"><strong>Matemática:</strong> ${this.textoCompleto.recomendacion_matematica}</p>
          <p><em>${this.textoCompleto.mensaje_motivacional}</em></p>
        `;
      } else {
        textoConstruido = this.textoCompleto;
      }
      this.iniciarTypewriter(textoConstruido);
    }
  }

  ngOnDestroy(): void {
    if (this.typewriterTimeout) clearTimeout(this.typewriterTimeout);
  }

  private iniciarTypewriter(textoBruto: string): void {
    // Parse markdown bold (**text**) to <strong>text</strong>
    const texto = textoBruto.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    this.consejoTexto.set('');
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
          i++; // Skip the '>'
        } else {
          currentHTML += texto[i];
          i++;
        }
        
        this.consejoTexto.set(currentHTML);
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
