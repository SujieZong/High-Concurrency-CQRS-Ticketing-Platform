
namespace CombinedShopService.Application;
using CombinedShopService.Domain.Entities;
using CombinedShopService.Shared;


public interface ITagApplication
{
    //get all tags
    Task<Result<IReadOnlyList<Tag>>> GetAllAsync(CancellationToken ct = default);

    // add tag, tag only
    Task<Result<Tag>> CreateTagAsync(string name, CancellationToken ct = default);

    //add tag and bind to event
    Task<Result<Tag>> CreateAndAttachToEventAsync(string name, int eventId, CancellationToken ct = default);

    // add tag to event
    Task<Result<Unit>> AttachToEventAsync(int tagId, int eventId, CancellationToken ct = default); 

    //Remove tag from Event
    Task<Result<Unit>> DetachFromEventAsync(int tagId, int eventId, CancellationToken ct = default);

    // get all events by tag
    Task<Result<string?>> GetNameByIdAsync(int tagId, CancellationToken ct = default);
}