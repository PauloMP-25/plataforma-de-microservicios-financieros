export interface ResumenFinancieroDTO {
  desde:               string;   // LocalDateTime → ISO string
  hasta:               string;
  totalIngresos:       number;
  totalGastos:         number;
  balance:             number;
  cantidadIngresos:    number;
  cantidadGastos:      number;
  totalTransacciones:  number;
  promedioIngreso:     number;
  promedioGasto:       number;
}