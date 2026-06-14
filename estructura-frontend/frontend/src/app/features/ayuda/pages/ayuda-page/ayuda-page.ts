import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface ContactoMock {
  tipo: string;
  valor: string;
  horario: string;
  badge: string;
  badgeClass: string;
  iconoClass: string;
  cardClass: string;
  iconWrapClass: string;
  ctaClass: string;
  textoBoton: string;
  href: string;
}

interface FaqMock {
  id: number;
  pregunta: string;
  respuesta: string;
  categoria: string;
  iconoClass: string;
  abierta: boolean;
}

@Component({
  selector: 'app-ayuda-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ayuda-page.html',
  styleUrl: './ayuda-page.scss'
})
export class AyudaPageComponent {

  busqueda: string = '';
  categoriaActiva: string = 'todos';

  categorias = [
    { id: 'todos',     label: 'Todos',     icono: 'fa-solid fa-border-all' },
    { id: 'cuenta',    label: 'Cuenta',    icono: 'fa-solid fa-user' },
    { id: 'pagos',     label: 'Pagos',     icono: 'fa-solid fa-credit-card' },
    { id: 'limites',   label: 'Límites',   icono: 'fa-solid fa-shield-halved' },
    { id: 'seguridad', label: 'Seguridad', icono: 'fa-solid fa-lock' },
  ];

  canalesContacto: ContactoMock[] = [
    {
      tipo: 'Correo Electrónico',
      valor: 'soporte@luka.pe',
      horario: 'Respuesta en 24 hrs',
      badge: '● En línea',
      badgeClass: 'badge-online',
      iconoClass: 'fa-solid fa-envelope',
      cardClass: 'card-email',
      iconWrapClass: 'icon-email',
      ctaClass: 'cta-email',
      textoBoton: 'Enviar correo',
      href: 'mailto:soporte@luka.pe'
    },
    {
      tipo: 'WhatsApp',
      valor: '+51 999 999 999',
      horario: 'Lun – Vie, 9am – 6pm',
      badge: '● Rápido',
      badgeClass: 'badge-fast',
      iconoClass: 'fa-brands fa-whatsapp',
      cardClass: 'card-whatsapp',
      iconWrapClass: 'icon-wa',
      ctaClass: 'cta-wa',
      textoBoton: 'Chatear ahora',
      href: 'https://wa.me/51999999999'
    },
    {
      tipo: 'Teléfono',
      valor: '01 800 000 (Lima)',
      horario: 'Lun – Vie, 9am – 5pm',
      badge: 'Horario',
      badgeClass: 'badge-horario',
      iconoClass: 'fa-solid fa-phone',
      cardClass: 'card-telefono',
      iconWrapClass: 'icon-tel',
      ctaClass: 'cta-tel',
      textoBoton: 'Llamar ahora',
      href: 'tel:+5101800000'
    }
  ];

  preguntasFrecuentes: FaqMock[] = [
    {
      id: 1,
      categoria: 'cuenta',
      pregunta: '¿Cómo edito mi perfil y datos personales?',
      respuesta: 'Ve a Configuración → Mi Perfil desde el menú lateral. Allí podrás actualizar tu nombre, correo electrónico, número de teléfono y foto de perfil. Los cambios se guardan al hacer clic en "Guardar cambios".',
      iconoClass: 'fa-solid fa-user-pen',
      abierta: false
    },
    {
      id: 2,
      categoria: 'cuenta',
      pregunta: '¿Cómo restablezco mi contraseña?',
      respuesta: 'En la pantalla de inicio de sesión, haz clic en "¿Olvidaste tu contraseña?". Ingresa tu correo y recibirás un enlace para crear una nueva contraseña en menos de 5 minutos. Si no llega, revisa tu carpeta de spam.',
      iconoClass: 'fa-solid fa-key',
      abierta: false
    },
    {
      id: 3,
      categoria: 'pagos',
      pregunta: '¿Cómo registro un nuevo método de pago?',
      respuesta: 'Ve a Configuración → Métodos de Pago y selecciona "Agregar tarjeta". Aceptamos tarjetas de débito y crédito Visa, Mastercard y American Express. También puedes vincular tu cuenta bancaria.',
      iconoClass: 'fa-solid fa-credit-card',
      abierta: false
    },
    {
      id: 4,
      categoria: 'pagos',
      pregunta: '¿Puedo solicitar un reembolso de una transacción?',
      respuesta: 'Sí. Tienes hasta 7 días hábiles para solicitar el reembolso. Ve a Mis Transacciones, selecciona la operación y haz clic en "Solicitar reembolso". El monto se acredita en 3 a 5 días hábiles.',
      iconoClass: 'fa-solid fa-rotate-left',
      abierta: false
    },
    {
      id: 5,
      categoria: 'limites',
      pregunta: '¿Cómo configuro mi límite de gasto mensual?',
      respuesta: 'Desde el menú principal ve a Límites de Gasto. Define el monto máximo mensual con el slider o ingresándolo manualmente. Elige el porcentaje de alerta y haz clic en "Guardar Límite".',
      iconoClass: 'fa-solid fa-shield-halved',
      abierta: false
    },
    {
      id: 6,
      categoria: 'limites',
      pregunta: '¿Cuándo y cómo recibo las alertas de límite?',
      respuesta: 'Las alertas se envían por notificación push y correo electrónico cuando tu gasto alcanza el porcentaje configurado (por defecto 80%). Puedes personalizar esto en Límites de Gasto → Porcentaje de Alerta.',
      iconoClass: 'fa-solid fa-bell',
      abierta: false
    },
    {
      id: 7,
      categoria: 'seguridad',
      pregunta: '¿Mis datos financieros están seguros en Luka?',
      respuesta: 'Sí. Luka utiliza encriptación AES-256 para proteger tu información financiera. Nunca almacenamos los datos completos de tu tarjeta; usamos tokenización. Además puedes activar 2FA en Configuración → Seguridad.',
      iconoClass: 'fa-solid fa-lock',
      abierta: false
    },
    {
      id: 8,
      categoria: 'seguridad',
      pregunta: '¿Cómo activo la verificación en dos pasos?',
      respuesta: 'Ve a Configuración → Seguridad → Verificación en dos pasos. Puedes elegir entre SMS o una app autenticadora como Google Authenticator o Authy. Cada inicio de sesión requerirá un código adicional.',
      iconoClass: 'fa-solid fa-mobile-screen',
      abierta: false
    }
  ];

  get preguntasFiltradas(): FaqMock[] {
    const q = this.busqueda.toLowerCase().trim();
    return this.preguntasFrecuentes.filter(faq => {
      const matchCat = this.categoriaActiva === 'todos' || faq.categoria === this.categoriaActiva;
      const matchQuery = !q || faq.pregunta.toLowerCase().includes(q) || faq.respuesta.toLowerCase().includes(q);
      return matchCat && matchQuery;
    });
  }

  seleccionarCategoria(id: string): void {
    this.categoriaActiva = id;
  }

  conmutarFaq(id: number): void {
    this.preguntasFrecuentes = this.preguntasFrecuentes.map(faq =>
      faq.id === id ? { ...faq, abierta: !faq.abierta } : { ...faq, abierta: false }
    );
  }
}