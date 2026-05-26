namespace My_Complex.DTOs
{
    public class RegisterRequest
    {
        public string Nombre { get; set; }
        public string Apellido { get; set; }
        public string Documento { get; set; }
        public string Email { get; set; }
        public string Password { get; set; }
        public string Rol { get; set; }
        public int? ApartamentoId { get; set; } // null para vigilantes y trabajadores
    }
}