export type ClusterType = 'FOODIE' | 'DIGITAL' | 'WELLNESS' | 'EXPLORER' | 'MINIMALISTA' | 'SHOPPER' | 'TECHIE' | 'SOCIAL';

export interface ClusterInfo {
  id: ClusterType;
  emoji: string;
  nombre: string;
  nombreDisplay: string;
  color: string;
  rasgos: string[];
  descripcion: string;
  descripcionCorta: string; // Nueva propiedad agregada
  porcentajePredeterminado: number;
  categorias: string[];
}

export const CLUSTERS_INFO: Record<ClusterType, ClusterInfo> = {
  FOODIE: {
    id: 'FOODIE',
    emoji: '🥑',
    nombre: 'El Foodie Explorador',
    nombreDisplay: 'FOODIE',
    color: '#f59e0b', // Amber
    rasgos: ['Gastrónomo', 'Social', 'Delivery Lover'],
    descripcion: 'Paulo, tras analizar tus movimientos, te he bautizado como **\'El Foodie Explorador\'**. Tienes un paladar exigente: el 65% de tus gastos no fijos se van en descubrir nuevos sabores en restaurantes y barras de café.\n\n**Valor de Salud:** Como tu perfil es gastronómico, podrías ahorrar un **15%** mensual si aprovechas los días de promociones bancarias en tus locales favoritos o si te pones un presupuesto semanal de \'salidas\' fijo. Ese ahorro extra de S/ 120.00 aceleraría tu meta de la **Laptop Gamer** en casi un mes. ¡Sigue explorando, pero con estrategia!',
    descripcionCorta: 'Gastos enfocados en salidas a restaurantes, cafeterías y servicios de delivery.',
    porcentajePredeterminado: 65,
    categorias: ['Restaurantes', 'Cafeterías', 'Delivery', 'Supermercados']
  },
  DIGITAL: {
    id: 'DIGITAL',
    emoji: '💻',
    nombre: 'El Techie Digital',
    nombreDisplay: 'DIGITAL',
    color: '#22d3ee', // Cyan
    rasgos: ['Gadgets', 'Streaming', 'Automatizado'],
    descripcion: 'Te encanta el software premium, los servicios en la nube y las suscripciones que optimizan tu productividad. Mantén un ojo en las suscripciones inactivas para evitar pérdidas silenciosas.',
    descripcionCorta: 'Consumo enfocado en suscripciones y servicios digitales.',
    porcentajePredeterminado: 48,
    categorias: ['Software', 'Streaming', 'Videojuegos', 'Telefonía']
  },
  WELLNESS: {
    id: 'WELLNESS',
    emoji: '🧘',
    nombre: 'El Gurú del Bienestar',
    nombreDisplay: 'WELLNESS',
    color: '#10b981', // Emerald
    rasgos: ['Salud', 'Fitness', 'Orgánico'],
    descripcion: 'Priorizas tu salud física y mental. Gastas considerablemente en gimnasios, comida orgánica y cuidado personal. Es una excelente inversión a largo plazo, mantén el equilibrio.',
    descripcionCorta: 'Enfoque en salud mental/física: gimnasios, comida orgánica y cuidado personal.',
    porcentajePredeterminado: 40,
    categorias: ['Gimnasio', 'Farmacia', 'Supermercados Orgánicos', 'Spas']
  },
  EXPLORER: {
    id: 'EXPLORER',
    emoji: '✈️',
    nombre: 'Aventurero del Mundo',
    nombreDisplay: 'EXPLORER',
    color: '#a855f7', // Purple
    rasgos: ['Viajero', 'Eventos', 'Coleccionista de Memorias'],
    descripcion: 'Prefieres atesorar recuerdos y vivencias antes que acumular cosas materiales. Tu dinero fluye hacia boletos de avión, conciertos y escapadas de fin de semana.',
    descripcionCorta: 'Gastos orientados a viajes, experiencias y eventos culturales.',
    porcentajePredeterminado: 60,
    categorias: ['Vuelos', 'Hospedaje', 'Conciertos', 'Transporte']
  },
  MINIMALISTA: {
    id: 'MINIMALISTA',
    emoji: '🧘',
    nombre: 'Esencialista Puro',
    nombreDisplay: 'MINIMALISTA',
    color: '#94a3b8', // Slate
    rasgos: ['Simple', 'Ahorrador', 'Compra Consciente'],
    descripcion: 'Valoras el espacio, el tiempo y la libertad financiera por encima del consumo. Tus compras son altamente filtradas y posees el mayor índice de ahorro de la comunidad Luka.',
    descripcionCorta: 'Priorización del ahorro y consumo basado estrictamente en necesidades esenciales.',
    porcentajePredeterminado: 75,
    categorias: ['Ahorros', 'Inversiones', 'Esenciales', 'Seguros']
  },
  SHOPPER: {
    id: 'SHOPPER',
    emoji: '🛍️',
    nombre: 'El Cazador de Ofertas',
    nombreDisplay: 'SHOPPER',
    color: '#ec4899', // Pink
    rasgos: ['Moda', 'Impulso', 'Promociones'],
    descripcion: 'Tus gastos revelan una fuerte inclinación hacia tiendas por departamento y moda. Analizamos que muchas compras suceden los fines de semana. Trata de aplicar la regla de las 48 horas antes de comprar.',
    descripcionCorta: 'Tendencia fuerte hacia tiendas de moda, retail y compras por impulso.',
    porcentajePredeterminado: 55,
    categorias: ['Ropa', 'Accesorios', 'Belleza', 'Regalos']
  },
  TECHIE: {
    id: 'TECHIE',
    emoji: '💻',
    nombre: 'El Early Adopter',
    nombreDisplay: 'TECHIE',
    color: '#3b82f6', // Blue
    rasgos: ['Gadgets', 'Suscripciones', 'Innovador'],
    descripcion: 'Tu inversión en tecnología, software y gadgets es dominante. Esto es genial para tu productividad, pero revisa tus suscripciones mensuales, podrías estar pagando por servicios redundantes.',
    descripcionCorta: 'Alto consumo en gadgets, software, videojuegos y suscripciones digitales.',
    porcentajePredeterminado: 48,
    categorias: ['Electrónica', 'Software', 'Videojuegos', 'Suscripciones']
  },
  SOCIAL: {
    id: 'SOCIAL',
    emoji: '🥂',
    nombre: 'El Alma de la Fiesta',
    nombreDisplay: 'SOCIAL',
    color: '#8b5cf6', // Purple
    rasgos: ['Eventos', 'Amigos', 'Nocturno'],
    descripcion: 'La mayor parte de tu capital se destina a experiencias sociales, conciertos y salidas grupales. Eres el centro de la diversión. Recomendamos presupuestar tus fines de semana para evitar sorpresas.',
    descripcionCorta: 'Inversión recurrente en experiencias, conciertos, bares y salidas grupales.',
    porcentajePredeterminado: 70,
    categorias: ['Bares', 'Conciertos', 'Eventos', 'Viajes Express']
  }
};

export const CLUSTERS_LIST: ClusterType[] = ['FOODIE', 'DIGITAL', 'WELLNESS', 'EXPLORER', 'MINIMALISTA', 'SHOPPER', 'TECHIE', 'SOCIAL'];
