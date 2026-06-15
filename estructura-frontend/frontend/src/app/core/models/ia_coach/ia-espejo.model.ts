export interface DatosPresenteDTO {
  scoreActual: number;
  saldoActual: number;
  metasActivas: number;
}

export interface ProyeccionHitoDTO {
  scoreProyectado: number;
  ahorroAcumulado: number;
  metasCumplidas: string[];
  metasFracasadas: string[];
}

export interface ProyeccionFuturaDTO {
  hitos3Meses: ProyeccionHitoDTO;
  hitos6Meses: ProyeccionHitoDTO;
  hitos12Meses: ProyeccionHitoDTO;
}

export interface NarrativasGeminiDTO {
  cartaContinuidad: string;
  cartaTransformacion: string;
}

export interface InsightEspejoTemporalDTO {
  datosPresente: DatosPresenteDTO;
  proyeccionContinuidad: ProyeccionFuturaDTO;
  proyeccionTransformacion: ProyeccionFuturaDTO;
  narrativasGemini: NarrativasGeminiDTO;
}
