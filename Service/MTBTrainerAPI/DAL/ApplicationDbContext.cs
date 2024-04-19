using Microsoft.EntityFrameworkCore;

namespace MTBTrainerAPI.DAL
{
    public class ApplicationDBContext : DbContext
    {
        public ApplicationDBContext(DbContextOptions<ApplicationDBContext> options)
            : base(options)
        {
        }

        public DbSet<SensorData> SensorData { get; set; }
    }
}