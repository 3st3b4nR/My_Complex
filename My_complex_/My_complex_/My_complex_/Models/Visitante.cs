using System.ComponentModel.DataAnnotations;

namespace My_complex_.Models
{
	public class Visitante
	{
		[Key]
		public int id_visitante { get; set; }
		public string Nombre { get; set; } = string.Empty;
		public string Apellido { get; set; } = string.Empty;
		public string Documento { get; set; } = string.Empty;
		public DateTime Fecha_Ingreso { get; set; }
		public DateTime? Fecha_Salida { get; set; }
	}
}
