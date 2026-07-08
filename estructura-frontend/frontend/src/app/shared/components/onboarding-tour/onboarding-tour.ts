import { Component, EventEmitter, Input, OnInit, Output, signal, computed, effect, HostListener, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TourStep {
  targetSelector: string;
  title: string;
  description: string;
  position: 'top' | 'bottom' | 'left' | 'right' | 'center';
}

@Component({
  selector: 'app-onboarding-tour',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './onboarding-tour.html',
  styleUrl: './onboarding-tour.scss'
})
export class OnboardingTour implements OnInit, AfterViewInit {
  @Input() steps: TourStep[] = [];
  @Output() cerrar = new EventEmitter<void>();

  @ViewChild('tooltipCard') tooltipCard!: ElementRef;

  readonly currentStepIndex = signal(0);
  readonly currentStep = computed(() => this.steps[this.currentStepIndex()]);
  readonly totalSteps = computed(() => this.steps.length);

  // Spotlight styles
  readonly spotlightStyle = signal<any>({
    top: '0px',
    left: '0px',
    width: '0px',
    height: '0px',
    opacity: '0',
    display: 'none'
  });

  // Tooltip position styles
  readonly tooltipStyle = signal<any>({
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    opacity: '0'
  });

  readonly activePosition = signal<'top' | 'bottom' | 'left' | 'right' | 'center'>('bottom');

  constructor() {
    // Recalculate positioning whenever step changes
    effect(() => {
      const index = this.currentStepIndex();
      if (this.steps.length > 0) {
        // Wait a tiny tick for DOM to render if needed
        setTimeout(() => this.calculateLayout(), 50);
      }
    });
  }

  ngOnInit(): void {
    if (this.steps.length === 0) {
      this.cerrar.emit();
    }
  }

  ngAfterViewInit(): void {
    this.calculateLayout();
  }

  @HostListener('window:resize')
  @HostListener('window:scroll')
  onWindowChange(): void {
    this.calculateLayout();
  }

  calculateLayout(): void {
    const step = this.currentStep();
    if (!step) return;

    let targetEl = document.querySelector(step.targetSelector) as HTMLElement;

    // Fallback if target element is not found
    if (!targetEl) {
      // Try to find common layout selectors or fallback to body
      targetEl = (document.querySelector('.lk-main') || 
                  document.querySelector('main') || 
                  document.querySelector('.page-container') || 
                  document.body) as HTMLElement;
    }

    // Scroll target into view if needed
    targetEl.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'smooth' });

    const rect = targetEl.getBoundingClientRect();
    const padding = 8; // Highlight padding

    // Set spotlight styles
    this.spotlightStyle.set({
      top: `${rect.top - padding}px`,
      left: `${rect.left - padding}px`,
      width: `${rect.width + padding * 2}px`,
      height: `${rect.height + padding * 2}px`,
      opacity: '1',
      display: 'block'
    });

    // Determine tooltip size (fallback defaults if not rendered yet)
    let tooltipWidth = 320;
    let tooltipHeight = 180;

    if (this.tooltipCard && this.tooltipCard.nativeElement) {
      const cardRect = this.tooltipCard.nativeElement.getBoundingClientRect();
      tooltipWidth = cardRect.width || 320;
      tooltipHeight = cardRect.height || 180;
    }

    let top = 0;
    let left = 0;
    let transform = 'none';

    const gap = 16; // Gap between spotlight and tooltip

    let computedPos = step.position;

    switch (step.position) {
      case 'top':
        top = rect.top - tooltipHeight - gap;
        left = rect.left + rect.width / 2 - tooltipWidth / 2;
        break;
      case 'bottom':
        top = rect.bottom + gap;
        left = rect.left + rect.width / 2 - tooltipWidth / 2;
        break;
      case 'left':
        top = rect.top + rect.height / 2 - tooltipHeight / 2;
        left = rect.left - tooltipWidth - gap;
        break;
      case 'right':
        top = rect.top + rect.height / 2 - tooltipHeight / 2;
        left = rect.left + rect.width + gap;
        break;
      case 'center':
      default:
        this.activePosition.set('center');
        // Center of the screen
        this.tooltipStyle.set({
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          position: 'fixed',
          opacity: '1'
        });
        return;
    }

    // Viewport bounds checking
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    // Boundary corrections
    if (left < 10) {
      left = 10;
    } else if (left + tooltipWidth > viewportWidth - 10) {
      left = viewportWidth - tooltipWidth - 10;
    }

    if (top < 10) {
      // If it goes off top, flip to bottom
      if (step.position === 'top') {
        top = rect.bottom + gap;
        computedPos = 'bottom';
      } else {
        top = 10;
      }
    } else if (top + tooltipHeight > viewportHeight - 10) {
      // If it goes off bottom, flip to top
      if (step.position === 'bottom') {
        top = rect.top - tooltipHeight - gap;
        computedPos = 'top';
      } else {
        top = viewportHeight - tooltipHeight - 10;
      }
    }

    this.activePosition.set(computedPos);

    this.tooltipStyle.set({
      top: `${top}px`,
      left: `${left}px`,
      transform: transform,
      position: 'fixed',
      opacity: '1'
    });
  }

  siguiente(): void {
    if (this.currentStepIndex() < this.totalSteps() - 1) {
      this.currentStepIndex.update(idx => idx + 1);
    } else {
      this.cerrarTour();
    }
  }

  atras(): void {
    if (this.currentStepIndex() > 0) {
      this.currentStepIndex.update(idx => idx - 1);
    }
  }

  cerrarTour(): void {
    this.cerrar.emit();
  }
}
