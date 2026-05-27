namespace My_Comunicacion.DTOs
{
    public class NoticiaResponse
    {
        public int Id { get; set; }              // Identificador de la noticia
        public string Titulo { get; set; }       // Título visible
        public string Contenido { get; set; }    // Texto de la noticia
        public DateTime Fecha { get; set; }      // Fecha de publicación
        public string Autor { get; set; }        // Nombre del usuario que la publicó
    }
}
