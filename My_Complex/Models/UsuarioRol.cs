namespace My_Complex.Models
{
    public class UsuarioRol
    {
        public int Usuario_id_usuario { get; set; }
        public Usuario Usuario { get; set; }

        public int Rol_id_rol { get; set; }
        public Rol Rol { get; set; }
    }
}
