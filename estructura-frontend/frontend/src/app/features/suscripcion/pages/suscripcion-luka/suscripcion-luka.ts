import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-suscripcion-luka',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="suscripcion-luka-placeholder">
      <i class="fa-solid fa-crown icon-placeholder"></i>
      <h2>Membresía Luka</h2>
      <p>Esta sección aún está en proceso. Pronto podrás gestionar tus planes, revisar tu membresía activa y administrar los beneficios de tu cuenta Luka desde aquí.</p>
    </div>
  `,
  styles: [`
    .suscripcion-luka-placeholder {
      padding: 4rem 2rem;
      text-align: center;
      border: 2px dashed color-mix(in srgb, var(--border-color) 60%, transparent);
      border-radius: 16px;
      margin: 3rem auto;
      max-width: 650px;
      background: linear-gradient(135deg, var(--bg-card) 0%, color-mix(in srgb, var(--bg-card) 92%, #000) 100%);
      color: var(--text-secondary);
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;
    }
    .icon-placeholder {
      font-size: 3rem;
      color: var(--color-primary);
      opacity: 0.6;
      margin-bottom: 0.5rem;
    }
    h2 {
      color: var(--text-primary);
      font-weight: 800;
      margin: 0;
      font-size: 1.5rem;
    }
    p {
      line-height: 1.6;
      margin: 0;
      font-size: 0.95rem;
    }
  `]
})
export class SuscripcionLuka {}
