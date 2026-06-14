import { Component, Input, OnChanges, SimpleChanges, OnDestroy, signal, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-habitos-coach',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-habitos-coach.html',
  styleUrl: './ia-habitos-coach.scss',
})
export class IaHabitosCoachComponent implements OnChanges, OnDestroy {
  @Input() consejo: string | null = '';

  consejoHtml = signal<string>('');
  consejoVisible = signal<boolean>(false);
  isWriting = signal<boolean>(false);

  private typewriterTimeout: any = null;

  constructor(private el: ElementRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['consejo'] && this.consejo) {
      this.iniciarTypewriter(this.consejo);
    }
  }

  ngOnDestroy(): void {
    if (this.typewriterTimeout) clearTimeout(this.typewriterTimeout);
  }

  private iniciarTypewriter(texto: string): void {
    if (!texto) return;
    this.consejoVisible.set(true);
    this.consejoHtml.set('');
    this.isWriting.set(true);
    
    // Auto-scroll al consejo para que el usuario no se pierda la animación
    setTimeout(() => {
      this.el.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);

    if (this.typewriterTimeout) clearTimeout(this.typewriterTimeout);

    // Parsear Markdown básico (negritas y saltos de línea)
    let parsed = texto.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    
    // Separar el texto si contiene "Hábito" en negrita
    const parts = parsed.split(/(?=<strong>H[aá]bito)/i);
    if (parts.length > 1) {
      // Remover saltos de línea extra al inicio del hábito
      let habitoText = parts.slice(1).join('').replace(/^(\\n|\n|<br>)+/, '');
      parsed = `<div class="analysis-text">${parts[0]}</div><div class="habit-highlight"><div class="habit-icon"><i class="fa-solid fa-bolt text-amber-400"></i></div><div class="habit-content">${habitoText}</div></div>`;
    } else {
      parsed = `<div class="analysis-text">${parsed}</div>`;
    }

    parsed = parsed.replace(/\\n/g, '<br>').replace(/\n/g, '<br>');
    
    // Tokenizar HTML para la animación
    const tokens: { type: string, value: string }[] = [];
    let isTag = false;
    let currentTag = '';
    
    for (let i = 0; i < parsed.length; i++) {
      const char = parsed[i];
      if (char === '<') {
        isTag = true;
        currentTag = char;
      } else if (char === '>') {
        isTag = false;
        currentTag += char;
        tokens.push({ type: 'tag', value: currentTag });
        currentTag = '';
      } else if (isTag) {
        currentTag += char;
      } else {
        tokens.push({ type: 'text', value: char });
      }
    }

    let i = 0;
    const escribir = () => {
      if (i < tokens.length) {
        let toAdd = '';
        while (i < tokens.length && tokens[i].type === 'tag') {
          toAdd += tokens[i].value;
          i++;
        }
        if (i < tokens.length && tokens[i].type === 'text') {
          toAdd += tokens[i].value;
          i++;
        }
        this.consejoHtml.update(prev => prev + toAdd);
        this.typewriterTimeout = setTimeout(escribir, 10);
      } else {
        this.isWriting.set(false);
      }
    };

    this.typewriterTimeout = setTimeout(escribir, 400);
  }
}
