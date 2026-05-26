using System.ComponentModel.DataAnnotations;

namespace My_Complex.Models
{
    public class Apartamento
    {
        [Key]
        public int ID { get; set; }

        public int Numero { get; set; }
        public int Torre { get; set; }
        public int Piso { get; set; }

        public int Usuario_id_usuario { get; set; }
        public Usuario Usuario { get; set; }
    }
}
