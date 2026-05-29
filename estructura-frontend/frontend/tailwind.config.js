/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        // Vinculamos los colores de Tailwind a nuestras variables globales CSS
        primary: {
          DEFAULT: 'var(--color-primary)',
          light: 'var(--color-primary-light)',
          dark: 'var(--color-primary-dark)',
          soft: 'var(--color-primary-soft)',
        },
        success: {
          DEFAULT: 'var(--color-success)',
          soft: 'var(--color-success-soft)',
        },
        danger: {
          DEFAULT: 'var(--color-danger)',
          soft: 'var(--color-danger-soft)',
        },
        warning: {
          DEFAULT: 'var(--color-warning)',
          soft: 'var(--color-warning-soft)',
        },
        info: {
          DEFAULT: 'var(--color-info)',
          soft: 'var(--color-info-soft)',
        },
        // Colores de categorías
        food: {
          DEFAULT: 'var(--color-food)',
          soft: 'var(--color-food-soft)',
        },
        transport: {
          DEFAULT: 'var(--color-transport)',
          soft: 'var(--color-transport-soft)',
        },
        study: {
          DEFAULT: 'var(--color-study)',
          soft: 'var(--color-study-soft)',
        },
        leisure: {
          DEFAULT: 'var(--color-leisure)',
          soft: 'var(--color-leisure-soft)',
        },
        health: {
          DEFAULT: 'var(--color-health)',
          soft: 'var(--color-health-soft)',
        },
        home: {
          DEFAULT: 'var(--color-home)',
          soft: 'var(--color-home-soft)',
        },
        // Neutros / Fondos
        body: 'var(--bg-body)',
        card: 'var(--bg-card)',
        sidebar: 'var(--bg-sidebar)',
        'text-primary': 'var(--text-primary)',
        'text-secondary': 'var(--text-secondary)',
        'text-muted': 'var(--text-muted)',
        border: 'var(--border-color)',
        'accent-soft-2': 'var(--color-accent-soft-2)',
        'surface-soft': 'var(--bg-surface-soft)',
      },
      fontFamily: {
        main: ['var(--font-main)', 'sans-serif'],
        heading: ['var(--font-heading)', 'sans-serif'],
      },
      borderRadius: {
        'sm': 'var(--radius-sm)',
        'md': 'var(--radius-md)',
        'lg': 'var(--radius-lg)',
        'xl': 'var(--radius-xl)',
      },
      boxShadow: {
        'sm': 'var(--shadow-sm)',
        'md': 'var(--shadow-md)',
        'lg': 'var(--shadow-lg)',
      },
      transitionProperty: {
        'custom': 'var(--transition)',
      }
    },
  },
  plugins: [],
}
