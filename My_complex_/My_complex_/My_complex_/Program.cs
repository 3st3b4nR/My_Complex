using Microsoft.EntityFrameworkCore;
using My_complex_.Config;
using My_complex_.Services; // asegúrate de importar el namespace correcto

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddDbContext<VisitantesDbContext>(options =>
	options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));

// 👇 Aquí registramos el servicio
builder.Services.AddScoped<VisitanteService>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

app.UseSwagger();
app.UseSwaggerUI();
app.MapControllers();
app.Run();
