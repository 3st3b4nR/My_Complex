using System.ComponentModel.DataAnnotations;

namespace My_complex_.Models
{
	public class RegistroVisita
	{
		[Key]
		public int id_registro { get; set; }
		public DateTime Fecha_Ingreso { get; set; }
		public DateTime? Fecha_Salida { get; set; }
		public int Visitante_id_visitante { get; set; }
	}
}
