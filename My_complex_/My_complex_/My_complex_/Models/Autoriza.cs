using System.ComponentModel.DataAnnotations;

namespace My_complex_.Models
{
	public class Autoriza
	{
		[Key]
		public int id_autorizacion { get; set; }
		public DateTime Fecha_autorizacion { get; set; }
		public string Estado { get; set; } = "Pendiente";
		public int Visitante_id_visitante { get; set; }
		public int Usuario_id { get; set; }
	}
}
