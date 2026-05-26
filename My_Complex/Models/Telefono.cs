using System.ComponentModel.DataAnnotations;

namespace My_Complex.Models
{
    public class Telefono
    {
        [Key]
        public int id_telefono { get; set; }

        public string NumeroTel { get; set; } = string.Empty;
        public string Tipo { get; set; } = string.Empty;

        public int Usuario_id_usuario { get; set; }
        public Usuario Usuario { get; set; }
    }
}
