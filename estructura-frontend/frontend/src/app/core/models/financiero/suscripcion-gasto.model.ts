/**
 * Modelo de Suscripción de Gastos
 * Representa suscripciones recurrentes como Netflix, Spotify, Gimnasio, etc.
 */

export type EstadoSuscripcion = 'ACTIVA' | 'PAUSADA' | 'VENCIDA';
export type FrecuenciaSuscripcion = 'DIARIO' | 'SEMANAL' | 'QUINCENAL' | 'MENSUAL' | 'TRIMESTRAL' | 'SEMESTRAL' | 'ANUAL';

export interface SuscripcionGasto {
  id: string;
  nombre: string;
  descripcion: string;
  categoria: string; // legacy category string or just alias
  categoriaId: string;
  monto: number;
  metodoPago: string;
  frecuencia: FrecuenciaSuscripcion;
  fechaInicio: string; // ISO 8601
  proximoVencimiento: string; // ISO 8601
  fechaUltimoPago?: string; // ISO 8601
  estado: EstadoSuscripcion;
  fechaCreacion: string;
  ultimaActualizacion: string;
}

export interface SuscripcionDTO extends SuscripcionGasto {
  // Propiedades derivadas para UI
  diasParaVencimiento?: number;
  vencePronto?: boolean;
  gastosEsteAno?: number;
  gastosEsteMes?: number;
}

/**
 * Request para crear o actualizar suscripción
 */
export interface CrearSuscripcionRequest {
  nombre: string;
  descripcion: string;
  categoria: string;
  categoriaId: string;
  monto: number;
  metodoPago: string;
  frecuencia: FrecuenciaSuscripcion;
  fechaInicio: string;
}

export interface ActualizarSuscripcionRequest extends CrearSuscripcionRequest {
  id: string;
  estado?: EstadoSuscripcion;
}

/**
 * Response del servidor
 */
export interface SuscripcionApiResponse {
  datos: SuscripcionDTO[];
  total: number;
  pagina: number;
  tamano: number;
}

/**
 * Resumen de suscripciones para dashboard
 */
export interface ResumenSuscripciones {
  totalActivas: number;
  gastoMensualEstimado: number;
  proximoPago: SuscripcionDTO | null;
  cantidadTotal: number;
  proximasFechas: SuscripcionDTO[];
}

/**
 * Categorías disponibles con sus colores
 */
export const CATEGORIAS_SUSCRIPCION = [
  { id: 'food', nombre: 'Alimentación', color: '#FF7043', icon: 'fa-utensils' },
  { id: 'transport', nombre: 'Transporte', color: '#42A5F5', icon: 'fa-car' },
  { id: 'health', nombre: 'Salud', color: '#26C6DA', icon: 'fa-heart-pulse' },
  { id: 'home', nombre: 'Vivienda', color: '#FFA726', icon: 'fa-house' },
  { id: 'leisure', nombre: 'Entretenimiento', color: '#AB47BC', icon: 'fa-gamepad' },
  { id: 'study', nombre: 'Educación', color: '#66BB6A', icon: 'fa-graduation-cap' }
];

/**
 * Frecuencias disponibles
 */
export const FRECUENCIAS_SUSCRIPCION: Array<{ id: FrecuenciaSuscripcion; nombre: string; diasAproximado: number }> = [
  { id: 'DIARIO', nombre: 'Diario', diasAproximado: 1 },
  { id: 'SEMANAL', nombre: 'Semanal', diasAproximado: 7 },
  { id: 'QUINCENAL', nombre: 'Quincenal', diasAproximado: 15 },
  { id: 'MENSUAL', nombre: 'Mensual', diasAproximado: 30 },
  { id: 'TRIMESTRAL', nombre: 'Trimestral', diasAproximado: 90 },
  { id: 'SEMESTRAL', nombre: 'Semestral', diasAproximado: 180 },
  { id: 'ANUAL', nombre: 'Anual', diasAproximado: 365 }
];

/**
 * Estados disponibles
 */
export const ESTADOS_SUSCRIPCION = [
  { id: 'ACTIVA', nombre: 'Activa', color: '#22C55E' },
  { id: 'PAUSADA', nombre: 'Pausada', color: '#F59E0B' },
  { id: 'VENCIDA', nombre: 'Vencida', color: '#EF4444' }
];


/**
 * Categorías de plataformas soportadas
 */
export type CategoriaSuscripcion =
  | 'streaming_video'
  | 'streaming_musica'
  | 'software_productividad'
  | 'almacenamiento'
  | 'gaming'
  | 'educacion'
  | 'telecomunicaciones'
  | 'delivery_transporte'
  | 'finanzas_seguridad'
  | 'inteligencia_artificial'
  | 'fitness_salud'
  | 'lectura_noticias'
  | 'redes_sociales';

/**
 * Plataforma de suscripción con icono, color de marca, aliases de búsqueda y categoría.
 * El campo `icon` contiene la clase FontAwesome (ej: 'fa-brands fa-netflix').
 * Si la plataforma no tiene icono de marca disponible, `icon` queda vacío
 * y se muestra un fallback con la inicial del nombre.
 */
export interface PlataformaSoportada {
  id: string;
  nombre: string;
  aliases: string[];
  categoria: CategoriaSuscripcion;
  color: string;
  icon: string;
}

export const PLATAFORMAS_SOPORTADAS: PlataformaSoportada[] = [
  // ── Streaming de video ─────────────────────────────────────────────────────
  { id: 'netflix',          nombre: 'Netflix',          aliases: ['netflix'],                                         categoria: 'streaming_video',       color: '#E50914', icon: 'fa-brands fa-netflix' },
  { id: 'disney-plus',      nombre: 'Disney+',           aliases: ['disney', 'disney plus', 'disney+'],               categoria: 'streaming_video',       color: '#113CCF', icon: 'fa-brands fa-disney' },
  { id: 'max',              nombre: 'Max',               aliases: ['max', 'hbo', 'hbo max'],                          categoria: 'streaming_video',       color: '#002BE7', icon: 'fa-solid fa-tv' },
  { id: 'prime-video',      nombre: 'Prime Video',       aliases: ['prime video', 'amazon prime', 'amazon'],          categoria: 'streaming_video',       color: '#00A8E1', icon: 'fa-brands fa-amazon' },
  { id: 'apple-tv',         nombre: 'Apple TV+',         aliases: ['apple tv', 'apple tv+', 'appletv'],               categoria: 'streaming_video',       color: '#000000', icon: 'fa-brands fa-apple' },
  { id: 'crunchyroll',      nombre: 'Crunchyroll',       aliases: ['crunchyroll'],                                     categoria: 'streaming_video',       color: '#F47521', icon: 'fa-solid fa-dragon' },
  { id: 'youtube-premium',  nombre: 'YouTube Premium',   aliases: ['youtube', 'youtube premium', 'yt premium'],       categoria: 'streaming_video',       color: '#FF0000', icon: 'fa-brands fa-youtube' },
  { id: 'paramount-plus',   nombre: 'Paramount+',        aliases: ['paramount', 'paramount plus', 'paramount+'],      categoria: 'streaming_video',       color: '#0064FF', icon: 'fa-solid fa-star' },
  { id: 'vix',              nombre: 'ViX',               aliases: ['vix'],                                             categoria: 'streaming_video',       color: '#00A0DC', icon: 'fa-solid fa-play' },

  // ── Streaming de música / audio ────────────────────────────────────────────
  { id: 'spotify',          nombre: 'Spotify',           aliases: ['spotify'],                                         categoria: 'streaming_musica',      color: '#1DB954', icon: 'fa-brands fa-spotify' },
  { id: 'apple-music',      nombre: 'Apple Music',       aliases: ['apple music', 'applemusic'],                       categoria: 'streaming_musica',      color: '#FA243C', icon: 'fa-brands fa-apple' },
  { id: 'youtube-music',    nombre: 'YouTube Music',     aliases: ['youtube music', 'yt music'],                       categoria: 'streaming_musica',      color: '#FF0000', icon: 'fa-brands fa-youtube' },
  { id: 'tidal',            nombre: 'Tidal',             aliases: ['tidal'],                                           categoria: 'streaming_musica',      color: '#000000', icon: 'fa-solid fa-music' },
  { id: 'deezer',           nombre: 'Deezer',            aliases: ['deezer'],                                          categoria: 'streaming_musica',      color: '#FEAA2D', icon: 'fa-solid fa-headphones' },
  { id: 'soundcloud',       nombre: 'SoundCloud',        aliases: ['soundcloud', 'soundcloud go'],                     categoria: 'streaming_musica',      color: '#FF5500', icon: 'fa-brands fa-soundcloud' },
  { id: 'audible',          nombre: 'Audible',           aliases: ['audible'],                                         categoria: 'streaming_musica',      color: '#F8991C', icon: 'fa-solid fa-book-open' },
  { id: 'amazon-music',     nombre: 'Amazon Music',      aliases: ['amazon music'],                                    categoria: 'streaming_musica',      color: '#00A8E1', icon: 'fa-brands fa-amazon' },

  // ── Software y productividad ───────────────────────────────────────────────
  { id: 'microsoft-365',    nombre: 'Microsoft 365',     aliases: ['microsoft 365', 'office 365', 'office', 'm365'],  categoria: 'software_productividad',color: '#D83B01', icon: 'fa-brands fa-microsoft' },
  { id: 'google-workspace', nombre: 'Google Workspace',  aliases: ['google workspace', 'gsuite', 'g suite'],          categoria: 'software_productividad',color: '#4285F4', icon: 'fa-brands fa-google' },
  { id: 'adobe-cc',         nombre: 'Adobe Creative Cloud',aliases: ['adobe', 'creative cloud', 'photoshop'],         categoria: 'software_productividad',color: '#FF0000', icon: 'fa-brands fa-adobe' },
  { id: 'notion',           nombre: 'Notion',            aliases: ['notion'],                                          categoria: 'software_productividad',color: '#000000', icon: 'fa-solid fa-n' },
  { id: 'canva',            nombre: 'Canva',             aliases: ['canva', 'canva pro'],                              categoria: 'software_productividad',color: '#00C4CC', icon: 'fa-solid fa-palette' },
  { id: 'figma',            nombre: 'Figma',             aliases: ['figma'],                                           categoria: 'software_productividad',color: '#F24E1E', icon: 'fa-brands fa-figma' },
  { id: 'slack',            nombre: 'Slack',             aliases: ['slack'],                                           categoria: 'software_productividad',color: '#4A154B', icon: 'fa-brands fa-slack' },
  { id: 'zoom',             nombre: 'Zoom',              aliases: ['zoom'],                                            categoria: 'software_productividad',color: '#0B5CFF', icon: 'fa-solid fa-video' },
  { id: 'github',           nombre: 'GitHub',            aliases: ['github', 'github pro'],                            categoria: 'software_productividad',color: '#181717', icon: 'fa-brands fa-github' },
  { id: 'dropbox',          nombre: 'Dropbox',           aliases: ['dropbox'],                                         categoria: 'almacenamiento',        color: '#0061FF', icon: 'fa-brands fa-dropbox' },
  { id: 'google-one',       nombre: 'Google One',        aliases: ['google one', 'google drive'],                      categoria: 'almacenamiento',        color: '#4285F4', icon: 'fa-brands fa-google-drive' },
  { id: 'icloud',           nombre: 'iCloud+',           aliases: ['icloud', 'icloud+', 'icloud plus'],                categoria: 'almacenamiento',        color: '#3693F3', icon: 'fa-brands fa-apple' },
  { id: 'onedrive',         nombre: 'OneDrive',          aliases: ['onedrive', 'one drive'],                           categoria: 'almacenamiento',        color: '#0078D4', icon: 'fa-brands fa-microsoft' },

  // ── Gaming ─────────────────────────────────────────────────────────────────
  { id: 'playstation-plus', nombre: 'PlayStation Plus',  aliases: ['playstation', 'ps plus', 'playstation plus','psn'],categoria: 'gaming',               color: '#003791', icon: 'fa-brands fa-playstation' },
  { id: 'xbox-game-pass',   nombre: 'Xbox Game Pass',    aliases: ['xbox', 'game pass', 'gamepass'],                   categoria: 'gaming',               color: '#107C10', icon: 'fa-brands fa-xbox' },
  { id: 'steam',            nombre: 'Steam',             aliases: ['steam'],                                           categoria: 'gaming',               color: '#1b2838', icon: 'fa-brands fa-steam' },
  { id: 'discord-nitro',    nombre: 'Discord Nitro',     aliases: ['discord', 'discord nitro', 'nitro'],               categoria: 'gaming',               color: '#5865F2', icon: 'fa-brands fa-discord' },
  { id: 'ea-play',          nombre: 'EA Play',           aliases: ['ea play', 'ea'],                                   categoria: 'gaming',               color: '#000000', icon: 'fa-solid fa-gamepad' },
  { id: 'nintendo',         nombre: 'Nintendo Switch Online',aliases: ['nintendo', 'switch online', 'nintendo switch'],categoria: 'gaming',               color: '#E60012', icon: 'fa-solid fa-gamepad' },

  // ── Educación ──────────────────────────────────────────────────────────────
  { id: 'utp',              nombre: 'UTP',               aliases: ['utp', 'universidad tecnologica del peru'],         categoria: 'educacion',            color: '#7A1FA2', icon: 'fa-solid fa-graduation-cap' },
  { id: 'coursera',         nombre: 'Coursera',          aliases: ['coursera'],                                        categoria: 'educacion',            color: '#0056D2', icon: 'fa-solid fa-book' },
  { id: 'udemy',            nombre: 'Udemy',             aliases: ['udemy'],                                           categoria: 'educacion',            color: '#A435F0', icon: 'fa-solid fa-book-open' },
  { id: 'duolingo',         nombre: 'Duolingo',          aliases: ['duolingo'],                                        categoria: 'educacion',            color: '#58CC02', icon: 'fa-solid fa-language' },
  { id: 'platzi',           nombre: 'Platzi',            aliases: ['platzi'],                                          categoria: 'educacion',            color: '#98CA3F', icon: 'fa-solid fa-graduation-cap' },
  { id: 'khan-academy',     nombre: 'Khan Academy',      aliases: ['khan academy', 'khan'],                            categoria: 'educacion',            color: '#14BF96', icon: 'fa-solid fa-chalkboard-teacher' },
  { id: 'crehana',          nombre: 'Crehana',           aliases: ['crehana'],                                         categoria: 'educacion',            color: '#4429E4', icon: 'fa-solid fa-graduation-cap' },

  // ── Telecomunicaciones (Perú) ──────────────────────────────────────────────
  { id: 'whatsapp-business',nombre: 'WhatsApp Business', aliases: ['whatsapp', 'whatsapp business'],                   categoria: 'telecomunicaciones',   color: '#25D366', icon: 'fa-brands fa-whatsapp' },
  { id: 'claro',            nombre: 'Claro',             aliases: ['claro'],                                           categoria: 'telecomunicaciones',   color: '#DA291C', icon: 'fa-solid fa-signal' },
  { id: 'movistar',         nombre: 'Movistar',          aliases: ['movistar'],                                        categoria: 'telecomunicaciones',   color: '#019DF4', icon: 'fa-solid fa-signal' },
  { id: 'entel',            nombre: 'Entel',             aliases: ['entel'],                                           categoria: 'telecomunicaciones',   color: '#0033A0', icon: 'fa-solid fa-signal' },
  { id: 'bitel',            nombre: 'Bitel',             aliases: ['bitel'],                                           categoria: 'telecomunicaciones',   color: '#EE2E24', icon: 'fa-solid fa-signal' },

  // ── Delivery y transporte ──────────────────────────────────────────────────
  { id: 'uber',             nombre: 'Uber',              aliases: ['uber'],                                            categoria: 'delivery_transporte',  color: '#000000', icon: 'fa-solid fa-car' },
  { id: 'uber-eats',        nombre: 'Uber Eats',         aliases: ['uber eats', 'ubereats'],                           categoria: 'delivery_transporte',  color: '#06C167', icon: 'fa-solid fa-motorcycle' },
  { id: 'rappi',            nombre: 'Rappi',             aliases: ['rappi', 'rappi prime'],                            categoria: 'delivery_transporte',  color: '#FF441F', icon: 'fa-solid fa-bag-shopping' },
  { id: 'didi',             nombre: 'DiDi',              aliases: ['didi'],                                            categoria: 'delivery_transporte',  color: '#FF7200', icon: 'fa-solid fa-car' },
  { id: 'pedidosya',        nombre: 'PedidosYa',         aliases: ['pedidosya', 'pedidos ya'],                         categoria: 'delivery_transporte',  color: '#FF0037', icon: 'fa-solid fa-motorcycle' },

  // ── Finanzas y seguridad ───────────────────────────────────────────────────
  { id: 'nordvpn',          nombre: 'NordVPN',           aliases: ['nordvpn', 'nord vpn'],                             categoria: 'finanzas_seguridad',   color: '#4687FF', icon: 'fa-solid fa-shield-halved' },
  { id: 'expressvpn',       nombre: 'ExpressVPN',        aliases: ['expressvpn', 'express vpn'],                       categoria: 'finanzas_seguridad',   color: '#DA3940', icon: 'fa-solid fa-shield-halved' },
  { id: 'lastpass',         nombre: 'LastPass',          aliases: ['lastpass'],                                        categoria: 'finanzas_seguridad',   color: '#D32D2D', icon: 'fa-solid fa-key' },
  { id: '1password',        nombre: '1Password',         aliases: ['1password', 'one password'],                       categoria: 'finanzas_seguridad',   color: '#3B66BC', icon: 'fa-solid fa-lock' },
  { id: 'norton',           nombre: 'Norton 360',        aliases: ['norton', 'norton 360'],                            categoria: 'finanzas_seguridad',   color: '#FFC800', icon: 'fa-solid fa-shield' },

  // ── Inteligencia artificial ────────────────────────────────────────────────
  { id: 'chatgpt-plus',     nombre: 'ChatGPT Plus',      aliases: ['chatgpt', 'chatgpt plus', 'openai'],               categoria: 'inteligencia_artificial',color: '#10A37F',icon: 'fa-solid fa-robot' },
  { id: 'claude-pro',       nombre: 'Claude Pro',        aliases: ['claude', 'claude pro', 'anthropic'],               categoria: 'inteligencia_artificial',color: '#D97757',icon: 'fa-solid fa-robot' },
  { id: 'midjourney',       nombre: 'Midjourney',        aliases: ['midjourney'],                                      categoria: 'inteligencia_artificial',color: '#000000',icon: 'fa-solid fa-wand-magic-sparkles' },
  { id: 'github-copilot',   nombre: 'GitHub Copilot',    aliases: ['copilot', 'github copilot'],                       categoria: 'inteligencia_artificial',color: '#000000',icon: 'fa-brands fa-github' },
  { id: 'perplexity',       nombre: 'Perplexity',        aliases: ['perplexity', 'perplexity pro'],                    categoria: 'inteligencia_artificial',color: '#20808D',icon: 'fa-solid fa-magnifying-glass-chart' },

  // ── Fitness y salud ────────────────────────────────────────────────────────
  { id: 'strava',           nombre: 'Strava',            aliases: ['strava'],                                          categoria: 'fitness_salud',        color: '#FC4C02', icon: 'fa-solid fa-person-running' },
  { id: 'nike-training',    nombre: 'Nike Training Club',aliases: ['nike', 'nike training'],                           categoria: 'fitness_salud',        color: '#000000', icon: 'fa-solid fa-dumbbell' },

  // ── Lectura y noticias ─────────────────────────────────────────────────────
  { id: 'kindle-unlimited', nombre: 'Kindle Unlimited',  aliases: ['kindle', 'kindle unlimited'],                     categoria: 'lectura_noticias',     color: '#FF9900', icon: 'fa-solid fa-book' },
  { id: 'medium',           nombre: 'Medium',            aliases: ['medium'],                                          categoria: 'lectura_noticias',     color: '#000000', icon: 'fa-brands fa-medium' },

  // ── Redes sociales premium ─────────────────────────────────────────────────
  { id: 'x-premium',        nombre: 'X Premium',         aliases: ['x', 'twitter', 'x premium', 'twitter blue'],      categoria: 'redes_sociales',       color: '#000000', icon: 'fa-brands fa-x-twitter' },
  { id: 'linkedin-premium', nombre: 'LinkedIn Premium',  aliases: ['linkedin', 'linkedin premium'],                    categoria: 'redes_sociales',       color: '#0A66C2', icon: 'fa-brands fa-linkedin' },
  { id: 'tinder-gold',      nombre: 'Tinder Gold',       aliases: ['tinder', 'tinder gold', 'tinder plus'],            categoria: 'redes_sociales',       color: '#FF6B6B', icon: 'fa-solid fa-fire' },
];

/**
 * Busca una plataforma normalizando el texto (minúsculas, sin tildes).
 * Devuelve el primer match o undefined si no encuentra.
 */
export function findPlatform(query: string): PlataformaSoportada | undefined {
  const normalized = query
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim();

  if (!normalized) return undefined;

  return PLATAFORMAS_SOPORTADAS.find((p) =>
    p.aliases.some(
      (alias) => alias === normalized || alias.includes(normalized) || normalized.includes(alias)
    )
  );
}

